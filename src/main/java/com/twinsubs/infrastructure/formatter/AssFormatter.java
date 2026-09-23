package com.twinsubs.infrastructure.formatter;

import com.twinsubs.domain.model.MergedSubtitleEntry;
import com.twinsubs.domain.model.PositionMode;
import com.twinsubs.domain.model.SubtitleLayout;
import com.twinsubs.domain.model.SubtitleStyle;

import java.util.List;
import java.util.Objects;

/**
 * Generates ASS (Advanced SubStation Alpha) content with custom styling and placement.
 */
public final class AssFormatter {

    /**
     * Formats merged subtitle entries into a complete ASS script file content.
     */
    public String format(List<MergedSubtitleEntry> entries,
                         SubtitleStyle primaryStyle,
                         SubtitleStyle secondaryStyle,
                         PositionMode positionMode) {

        return format(entries, primaryStyle, secondaryStyle, SubtitleLayout.defaultLayout(positionMode));
    }

    public String format(List<MergedSubtitleEntry> entries,
                         SubtitleStyle primaryStyle,
                         SubtitleStyle secondaryStyle,
                         SubtitleLayout layout) {

        Objects.requireNonNull(entries, "Entries cannot be null");
        Objects.requireNonNull(primaryStyle, "Primary style cannot be null");
        Objects.requireNonNull(secondaryStyle, "Secondary style cannot be null");
        Objects.requireNonNull(layout, "Subtitle layout cannot be null");

        PositionMode positionMode = layout.positionMode();

        StringBuilder sb = new StringBuilder();

        // 1. Script Header
        sb.append("[Script Info]\n")
          .append("Title: twinsubs Bilingual Subtitles\n")
          .append("ScriptType: v4.00+\n")
          .append("PlayResX: 1920\n")
          .append("PlayResY: 1080\n")
          .append("ScaledBorderAndShadow: yes\n\n");

        // 2. Styles section definition
        sb.append("[V4+ Styles]\n")
          .append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n");

        // Alignment & MarginV values according to position mode and layout order
        int primaryAlign = (positionMode == PositionMode.BOTH_TOP) ? 8 : 2;
        int secondaryAlign = (positionMode == PositionMode.TOP_AND_BOTTOM) ? 8 : primaryAlign;

        int primaryMarginV = 35;
        int secondaryMarginV = 35;

        // Clean adaptive spacing: calculate clearance based on the font size of the bottom track
        int baseMargin = 45;
        if (positionMode == PositionMode.BOTH_BOTTOM) {
            int clearance = baseMargin + (layout.isFirstTrackPrimary() ? secondaryStyle.getFontSize() : primaryStyle.getFontSize()) * 2;
            primaryMarginV = layout.isFirstTrackPrimary() ? clearance : baseMargin;
            secondaryMarginV = layout.isFirstTrackPrimary() ? baseMargin : clearance;
        } else if (positionMode == PositionMode.BOTH_TOP) {
            int clearance = baseMargin + (layout.isFirstTrackPrimary() ? primaryStyle.getFontSize() : secondaryStyle.getFontSize()) * 2;
            primaryMarginV = layout.isFirstTrackPrimary() ? baseMargin : clearance;
            secondaryMarginV = layout.isFirstTrackPrimary() ? clearance : baseMargin;
        }
        sb.append(buildStyleLine("PrimaryStyle", primaryStyle, primaryAlign, primaryMarginV)).append("\n");
        sb.append(buildStyleLine("SecondaryStyle", secondaryStyle, secondaryAlign, secondaryMarginV)).append("\n\n");

        // 3. Events section
        sb.append("[Events]\n")
          .append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");

        for (MergedSubtitleEntry entry : entries) {
            String assText = formatDialogueText(entry, primaryStyle, secondaryStyle, layout);

            // Assign separate Layers (1 and 2) to disable collision pushing between the two independent tracks
            int layer = 1;
            String styleName = "PrimaryStyle";
            if (entry.getSecondaryText().isPresent() && entry.getPrimaryText().isEmpty()) {
                styleName = "SecondaryStyle";
                layer = 2;
            } else if (entry.getPrimaryText().isPresent() && entry.getSecondaryText().isEmpty()) {
                styleName = "PrimaryStyle";
                layer = 1;
            } else if (entry.getPrimaryText().isPresent() && entry.getSecondaryText().isPresent()) {
                styleName = "PrimaryStyle";
                layer = 1;
            }
            sb.append(String.format("Dialogue: %d,%s,%s,%s,,0,0,0,,%s\n",
                layer,
                formatAssTimestamp(entry.getStartTimeMs()),
                formatAssTimestamp(entry.getEndTimeMs()),
                styleName,
                assText
            ));
        }

        return sb.toString();
    }
    private String buildStyleLine(String styleName, SubtitleStyle style, int alignment, int marginV) {
        return String.format("Style: %s,%s,%d,%s,%s,&H00000000,&H80000000,%d,%d,0,0,100,100,0,0,1,2,1,%d,40,40,%d,1",
            styleName,
            style.getFontName(),
            style.getFontSize(),
            formatHexToAssColor(style.getHexColor()),
            formatHexToAssColor(style.getHexColor()),
            style.isBold() ? 1 : 0,
            style.isItalic() ? 1 : 0,
            alignment,
            marginV
        );
    }

    private String formatDialogueText(MergedSubtitleEntry entry,
                                     SubtitleStyle primaryStyle,
                                     SubtitleStyle secondaryStyle,
                                     SubtitleLayout layout) {

        PositionMode positionMode = layout.positionMode();

        // In TOP_AND_BOTTOM mode, we strictly force layout positioning, so ignore native tags.
        // In other modes, honor native positioning tags if present.
        boolean isTopAndBottom = (positionMode == PositionMode.TOP_AND_BOTTOM);
        boolean primaryHasPosition = !isTopAndBottom && hasPositionTag(entry.getPrimaryText().orElse(""));
        boolean secondaryHasPosition = !isTopAndBottom && hasPositionTag(entry.getSecondaryText().orElse(""));

        String primaryFormatted = entry.getPrimaryText()
            .map(t -> applyInlineStyle(escapeRawText(t, positionMode, primaryHasPosition), primaryStyle))
            .orElse("");

        String secondaryFormatted = entry.getSecondaryText()
            .map(t -> applyInlineStyle(escapeRawText(t, positionMode, secondaryHasPosition), secondaryStyle))
            .orElse("");

        if (!primaryFormatted.isEmpty() && !secondaryFormatted.isEmpty()) {
            if (positionMode == PositionMode.TOP_AND_BOTTOM) {
                String topText = layout.isFirstTrackPrimary() ? primaryFormatted : secondaryFormatted;
                String bottomText = layout.isFirstTrackPrimary() ? secondaryFormatted : primaryFormatted;
                return "{\\an8}" + topText + "\\N{\\an2}" + bottomText;
            } else {
                String firstText = layout.isFirstTrackPrimary() ? primaryFormatted : secondaryFormatted;
                String secondText = layout.isFirstTrackPrimary() ? secondaryFormatted : primaryFormatted;
                return firstText + "\\N" + secondText;
            }
        } else if (!primaryFormatted.isEmpty()) {
            return (positionMode == PositionMode.TOP_AND_BOTTOM ? "{\\an2}" : "") + primaryFormatted;
        } else {
            return (positionMode == PositionMode.TOP_AND_BOTTOM && !secondaryHasPosition ? "{\\an8}" : "") + secondaryFormatted;
        }
    }

    private boolean hasPositionTag(String text) {
        return text != null && text.matches("(?s).*\\{\\\\(an\\d|pos|move|org)[^}]*\\}.*");
    }

    private String applyInlineStyle(String text, SubtitleStyle style) {
        // Tag format: {\c&HBBGGRR&\fsSize\b1/0\i1/0}
        String colorTag = "\\c" + formatHexToAssColor(style.getHexColor());
        String fontTag = "\\fs" + style.getFontSize();

        boolean hasBoldHtml = text.contains("<b>") || text.contains("<strong>") || text.contains("</b>") || text.contains("</strong>");
        boolean hasItalicHtml = text.contains("<i>") || text.contains("<em>") || text.contains("</i>") || text.contains("</em>");

        String cleanText = text.replace("<b>", "{\\b1}").replace("</b>", "{\\b0}")
                               .replace("<strong>", "{\\b1}").replace("</strong>", "{\\b0}")
                               .replace("<i>", "{\\i1}").replace("</i>", "{\\i0}")
                               .replace("<em>", "{\\i1}").replace("</em>", "{\\i0}");
        String boldTag = hasBoldHtml ? "" : ("\\b" + (style.isBold() ? "1" : "0"));
        String italicTag = hasItalicHtml ? "" : ("\\i" + (style.isItalic() ? "1" : "0"));

        return "{" + colorTag + fontTag + boldTag + italicTag + "}" + cleanText;
    }

    private String escapeRawText(String text, PositionMode positionMode, boolean hasPositionTag) {
        String cleaned = text;
        // In TOP_AND_BOTTOM mode, strip all positioning tags.
        // In other modes, strip positioning tags ONLY IF the individual line doesn't explicitly declare its own position tag.
        if (positionMode == PositionMode.TOP_AND_BOTTOM || !hasPositionTag) {
            cleaned = cleaned.replaceAll("\\{\\\\(an\\d|pos|move|org)[^}]*\\}", "");
        }
        return cleaned.replace("\\", "\\\\").replace("\n", "\\N");
    }

    private String formatHexToAssColor(String hexColor) {
        String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
        String r = hex.substring(0, 2);
        String g = hex.substring(2, 4);
        String b = hex.substring(4, 6);
        return "&H00" + b + g + r + "&"; // BGR format for ASS
    }

    private String formatAssTimestamp(long ms) {
        long h = ms / 3600000;
        long rem = ms % 3600000;
        long m = rem / 60000;
        rem %= 60000;
        long s = rem / 1000;
        long cs = (rem % 1000) / 10; // Centiseconds (2 digits)

        return String.format("%d:%02d:%02d.%02d", h, m, s, cs);
    }
}


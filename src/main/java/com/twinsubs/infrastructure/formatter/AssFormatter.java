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

        // Alignment values according to position mode (ASS numpad notation)
        // 2 = Bottom center, 8 = Top center
        int primaryAlign = (positionMode == PositionMode.BOTH_TOP) ? 8 : 2;
        int secondaryAlign = (positionMode == PositionMode.TOP_AND_BOTTOM) ? 8 : primaryAlign;

        sb.append(buildStyleLine("PrimaryStyle", primaryStyle, primaryAlign)).append("\n");
        sb.append(buildStyleLine("SecondaryStyle", secondaryStyle, secondaryAlign)).append("\n\n");

        // 3. Events section
        sb.append("[Events]\n")
          .append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");

        for (MergedSubtitleEntry entry : entries) {
            String assText = formatDialogueText(entry, primaryStyle, secondaryStyle, layout);
            sb.append(String.format("Dialogue: 0,%s,%s,Default,,0,0,0,,%s\n",
                formatAssTimestamp(entry.getStartTimeMs()),
                formatAssTimestamp(entry.getEndTimeMs()),
                assText
            ));
        }

        return sb.toString();
    }

    private String buildStyleLine(String styleName, SubtitleStyle style, int alignment) {
        return String.format("Style: %s,%s,%d,%s,%s,&H00000000,&H80000000,%d,%d,0,0,100,100,0,0,1,2,1,%d,40,40,35,1",
            styleName,
            style.getFontName(),
            style.getFontSize(),
            formatHexToAssColor(style.getHexColor()),
            formatHexToAssColor(style.getHexColor()),
            style.isBold() ? 1 : 0,
            style.isItalic() ? 1 : 0,
            alignment
        );
    }

    private String formatDialogueText(MergedSubtitleEntry entry,
                                     SubtitleStyle primaryStyle,
                                     SubtitleStyle secondaryStyle,
                                     SubtitleLayout layout) {

        PositionMode positionMode = layout.positionMode();

        String primaryFormatted = entry.getPrimaryText()
            .map(t -> applyInlineStyle(escapeRawText(t), primaryStyle))
            .orElse("");

        String secondaryFormatted = entry.getSecondaryText()
            .map(t -> applyInlineStyle(escapeRawText(t), secondaryStyle))
            .orElse("");

        if (!primaryFormatted.isEmpty() && !secondaryFormatted.isEmpty()) {
            if (positionMode == PositionMode.TOP_AND_BOTTOM) {
                String topText = layout.isFirstTrackPrimary() ? primaryFormatted : secondaryFormatted;
                String bottomText = layout.isFirstTrackPrimary() ? secondaryFormatted : primaryFormatted;
                return "{\\an8}" + topText + "\\N" + bottomText;
            } else {
                String firstText = layout.isFirstTrackPrimary() ? primaryFormatted : secondaryFormatted;
                String secondText = layout.isFirstTrackPrimary() ? secondaryFormatted : primaryFormatted;
                return firstText + "\\N" + secondText;
            }
        } else if (!primaryFormatted.isEmpty()) {
            return primaryFormatted;
        } else {
            return (positionMode == PositionMode.TOP_AND_BOTTOM ? "{\\an8}" : "") + secondaryFormatted;
        }
    }

    private String applyInlineStyle(String text, SubtitleStyle style) {
        // Tag format: {\c&HBBGGRR&\fsSize\b1/0\i1/0}
        String colorTag = "\\c" + formatHexToAssColor(style.getHexColor());
        String fontTag = "\\fs" + style.getFontSize();
        String boldTag = "\\b" + (style.isBold() ? "1" : "0");
        String italicTag = "\\i" + (style.isItalic() ? "1" : "0");

        return "{" + colorTag + fontTag + boldTag + italicTag + "}" + text;
    }

    private String escapeRawText(String text) {
        return text.replace("\\", "\\\\").replace("\n", "\\N");
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

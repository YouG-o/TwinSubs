package com.twinsubs.domain.model;

import java.util.Objects;

/**
 * Immutable value object holding custom styling attributes for a subtitle track.
 */
public final class SubtitleStyle {

    private final String fontName;
    private final int fontSize;
    private final String hexColor; // Hexadecimal format "#RRGGBB"
    private final boolean bold;
    private final boolean italic;
    private final boolean outlineEnabled;
    private final int outlineThickness; // 0 to 5
    private final String outlineHexColor;
    private final boolean backgroundEnabled;
    private final String backgroundHexColor;
    private final int backgroundOpacity; // 0 to 100

    public SubtitleStyle(String fontName, int fontSize, String hexColor, boolean bold, boolean italic,
                         boolean outlineEnabled, int outlineThickness, String outlineHexColor,
                         boolean backgroundEnabled, String backgroundHexColor, int backgroundOpacity) {
        this.fontName = Objects.requireNonNull(fontName, "Font name cannot be null");
        if (fontSize <= 0) {
            throw new IllegalArgumentException("Font size must be positive: " + fontSize);
        }
        this.fontSize = fontSize;
        this.hexColor = sanitizeHexColor(hexColor);
        this.bold = bold;
        this.italic = italic;
        this.outlineEnabled = outlineEnabled;
        this.outlineThickness = Math.clamp(outlineThickness, 0, 5);
        this.outlineHexColor = sanitizeHexColor(outlineHexColor);
        this.backgroundEnabled = backgroundEnabled;
        this.backgroundHexColor = sanitizeHexColor(backgroundHexColor);
        this.backgroundOpacity = Math.clamp(backgroundOpacity, 0, 100);
    }

    private static String sanitizeHexColor(String hex) {
        if (hex == null || !hex.matches("^#?[0-9a-fA-F]{6}$")) {
            throw new IllegalArgumentException("Invalid hex color code: " + hex);
        }
        return hex.startsWith("#") ? hex.toUpperCase() : "#" + hex.toUpperCase();
    }

    public String getFontName() {
        return fontName;
    }

    public int getFontSize() {
        return fontSize;
    }

    public String getHexColor() {
        return hexColor;
    }

    public boolean isBold() {
        return bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public boolean isOutlineEnabled() {
        return outlineEnabled;
    }

    public int getOutlineThickness() {
        return outlineThickness;
    }

    public String getOutlineHexColor() {
        return outlineHexColor;
    }

    public boolean isBackgroundEnabled() {
        return backgroundEnabled;
    }

    public String getBackgroundHexColor() {
        return backgroundHexColor;
    }

    public int getBackgroundOpacity() {
        return backgroundOpacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubtitleStyle style = (SubtitleStyle) o;
        return fontSize == style.fontSize &&
               bold == style.bold &&
               italic == style.italic &&
               outlineEnabled == style.outlineEnabled &&
               outlineThickness == style.outlineThickness &&
               backgroundEnabled == style.backgroundEnabled &&
               backgroundOpacity == style.backgroundOpacity &&
               Objects.equals(fontName, style.fontName) &&
               Objects.equals(hexColor, style.hexColor) &&
               Objects.equals(outlineHexColor, style.outlineHexColor) &&
               Objects.equals(backgroundHexColor, style.backgroundHexColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fontName, fontSize, hexColor, bold, italic, outlineEnabled, outlineThickness, outlineHexColor, backgroundEnabled, backgroundHexColor, backgroundOpacity);
    }
}
package com.twinsubs.ui.util;

import javafx.scene.paint.Color;

/**
 * Helper utility for JavaFX Color conversions.
 */
public final class ColorUtils {

    private ColorUtils() {
        // Utility class
    }

    /**
     * Converts a JavaFX Color instance into a standard hex string "#RRGGBB".
     */
    public static String toHexString(Color color) {
        if (color == null) {
            return "#FFFFFF";
        }
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
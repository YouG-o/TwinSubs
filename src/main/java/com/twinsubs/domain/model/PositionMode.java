package com.twinsubs.domain.model;

/**
 * Supported screen positioning layouts for bilingual subtitle rendering.
 */
public enum PositionMode {
    /**
     * Both subtitle tracks positioned at the bottom of the screen.
     */
    BOTH_BOTTOM,

    /**
     * Both subtitle tracks positioned at the top of the screen.
     */
    BOTH_TOP,

    /**
     * Secondary track at top of screen, primary track at bottom.
     */
    TOP_AND_BOTTOM
}
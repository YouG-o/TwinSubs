package com.twinsubs.application.dto;

/**
 * Output target options for generated bilingual subtitles.
 */
public enum OutputOption {
    /**
     * Generate an external .ass file next to the input video.
     */
    EXTERNAL_ASS,

    /**
     * Embed the generated ASS track directly into the MKV container (remuxing).
     */
    EMBED_IN_MKV
}
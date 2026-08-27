package com.twinsubs.domain.model;

import java.util.Objects;

/**
 * Describes subtitle placement and which track is rendered first.
 */
public record SubtitleLayout(PositionMode positionMode, boolean primaryFirst) {

    public SubtitleLayout {
        Objects.requireNonNull(positionMode, "Position mode cannot be null");
    }

    public static SubtitleLayout defaultLayout(PositionMode positionMode) {
        return new SubtitleLayout(positionMode, false);
    }

    public boolean isFirstTrackPrimary() {
        return primaryFirst;
    }
}
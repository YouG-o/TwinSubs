package com.twinsubs.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a matched/merged subtitle entry holding timing and optional primary/secondary texts.
 * Pure domain model, independent of ASS syntax formatting.
 */
public final class MergedSubtitleEntry {

    private final long startTimeMs;
    private final long endTimeMs;
    private final String primaryText;
    private final String secondaryText;

    public MergedSubtitleEntry(long startTimeMs, long endTimeMs, String primaryText, String secondaryText) {
        if (startTimeMs < 0 || endTimeMs < startTimeMs) {
            throw new IllegalArgumentException("Invalid timestamps: start=" + startTimeMs + ", end=" + endTimeMs);
        }
        if ((primaryText == null || primaryText.isBlank()) && (secondaryText == null || secondaryText.isBlank())) {
            throw new IllegalArgumentException("At least one text (primary or secondary) must be present.");
        }
        this.startTimeMs = startTimeMs;
        this.endTimeMs = endTimeMs;
        this.primaryText = primaryText != null ? primaryText.trim() : null;
        this.secondaryText = secondaryText != null ? secondaryText.trim() : null;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public long getEndTimeMs() {
        return endTimeMs;
    }

    public Optional<String> getPrimaryText() {
        return Optional.ofNullable(primaryText);
    }

    public Optional<String> getSecondaryText() {
        return Optional.ofNullable(secondaryText);
    }

    public boolean hasBoth() {
        return primaryText != null && !primaryText.isBlank() 
            && secondaryText != null && !secondaryText.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MergedSubtitleEntry that = (MergedSubtitleEntry) o;
        return startTimeMs == that.startTimeMs &&
               endTimeMs == that.endTimeMs &&
               Objects.equals(primaryText, that.primaryText) &&
               Objects.equals(secondaryText, that.secondaryText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTimeMs, endTimeMs, primaryText, secondaryText);
    }
}
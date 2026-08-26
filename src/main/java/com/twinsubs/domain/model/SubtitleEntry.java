package com.twinsubs.domain.model;

import java.util.Objects;

/**
 * Represents an individual subtitle entry with timing in milliseconds and text content.
 */
public final class SubtitleEntry {

    private final long startTimeMs;
    private final long endTimeMs;
    private final String text;

    public SubtitleEntry(long startTimeMs, long endTimeMs, String text) {
        if (startTimeMs < 0 || endTimeMs < startTimeMs) {
            throw new IllegalArgumentException(
                "Invalid timestamps: start=" + startTimeMs + ", end=" + endTimeMs
            );
        }
        this.startTimeMs = startTimeMs;
        this.endTimeMs = endTimeMs;
        this.text = Objects.requireNonNull(text, "Text cannot be null").trim();
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public long getEndTimeMs() {
        return endTimeMs;
    }

    public String getText() {
        return text;
    }

    public long getDurationMs() {
        return endTimeMs - startTimeMs;
    }

    /**
     * Calculates temporal overlap in milliseconds with another subtitle entry.
     */
    public long calculateOverlapMs(SubtitleEntry other) {
        if (other == null) {
            return 0;
        }
        long overlapStart = Math.max(this.startTimeMs, other.startTimeMs);
        long overlapEnd = Math.min(this.endTimeMs, other.endTimeMs);
        return Math.max(0, overlapEnd - overlapStart);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubtitleEntry entry = (SubtitleEntry) o;
        return startTimeMs == entry.startTimeMs &&
               endTimeMs == entry.endTimeMs &&
               Objects.equals(text, entry.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTimeMs, endTimeMs, text);
    }

    @Override
    public String toString() {
        return "SubtitleEntry{" +
               "start=" + startTimeMs +
               ", end=" + endTimeMs +
               ", text='" + text + '\'' +
               '}';
    }
}
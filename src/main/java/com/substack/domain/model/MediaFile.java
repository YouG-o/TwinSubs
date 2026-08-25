package com.substack.domain.model;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents an input media container file along with its discovered subtitle tracks.
 */
public final class MediaFile {

    private final Path filePath;
    private final List<SubtitleTrack> tracks;

    public MediaFile(Path filePath, List<SubtitleTrack> tracks) {
        this.filePath = Objects.requireNonNull(filePath, "File path cannot be null");
        this.tracks = Collections.unmodifiableList(Objects.requireNonNull(tracks, "Tracks list cannot be null"));
    }

    public Path getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return filePath.getFileName().toString();
    }

    public List<SubtitleTrack> getTracks() {
        return tracks;
    }

    public boolean isMkv() {
        return getFileName().toLowerCase().endsWith(".mkv");
    }

    public boolean isMp4() {
        return getFileName().toLowerCase().endsWith(".mp4");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaFile mediaFile = (MediaFile) o;
        return Objects.equals(filePath, mediaFile.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath);
    }
}
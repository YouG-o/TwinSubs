package com.twinsubs.domain.model;

import java.util.Objects;

/**
 * Represents a subtitle track stream found within a media file.
 */
public final class SubtitleTrack {

    private final int index;
    private final String language;
    private final String title;
    private final String codecName;

    public SubtitleTrack(int index, String language, String title, String codecName) {
        this.index = index;
        this.language = language != null ? language : "und";
        this.title = title != null ? title : "";
        this.codecName = codecName != null ? codecName : "";
    }

    public int getIndex() {
        return index;
    }

    public String getLanguage() {
        return language;
    }

    public String getTitle() {
        return title;
    }

    public String getCodecName() {
        return codecName;
    }

    public String getDisplayName() {
        StringBuilder builder = new StringBuilder();
        if (!language.isBlank()) {
            builder.append("[").append(language.toUpperCase()).append("] ");
        }
        if (!title.isBlank()) {
            builder.append(title);
        } else {
            builder.append("Track #").append(index);
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubtitleTrack track = (SubtitleTrack) o;
        return index == track.index &&
               Objects.equals(language, track.language) &&
               Objects.equals(title, track.title) &&
               Objects.equals(codecName, track.codecName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, language, title, codecName);
    }
}
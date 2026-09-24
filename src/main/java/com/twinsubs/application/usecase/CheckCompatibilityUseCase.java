package com.twinsubs.application.usecase;

import com.twinsubs.domain.model.MediaFile;
import com.twinsubs.domain.model.SubtitleTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Use case validating if a batch of media files shares a compatible subtitle track structure.
 */
public final class CheckCompatibilityUseCase {

    /**
     * Checks whether all media files in a selection share a common subset of subtitle tracks.
     * Requires at least two common tracks available across all files.
     *
     * @param mediaFiles List of media files to check.
     * @return true if files share enough common tracks, false otherwise.
     */
    public boolean isCompatibleBatch(List<MediaFile> mediaFiles) {
        Objects.requireNonNull(mediaFiles, "Media files list cannot be null");

        if (mediaFiles.isEmpty() || mediaFiles.size() == 1) {
            return true;
        }

        // Find the intersection of tracks present in all files
        List<SubtitleTrack> commonTracks = getCommonTracks(mediaFiles);
        return commonTracks.size() >= 2;
    }

    /**
     * Returns the list of subtitle tracks that are present in ALL media files.
     */
    public List<SubtitleTrack> getCommonTracks(List<MediaFile> mediaFiles) {
        if (mediaFiles.isEmpty()) {
            return List.of();
        }

        List<SubtitleTrack> common = new ArrayList<>(mediaFiles.get(0).getTracks());

        for (int i = 1; i < mediaFiles.size(); i++) {
            List<SubtitleTrack> currentTracks = mediaFiles.get(i).getTracks();
            common.removeIf(ref -> currentTracks.stream().noneMatch(curr -> matchesTrack(ref, curr)));
        }

        return common;
    }

    public boolean isTrackAvailableInAll(SubtitleTrack track, List<MediaFile> mediaFiles) {
        if (track == null || mediaFiles.isEmpty()) {
            return false;
        }
        for (MediaFile file : mediaFiles) {
            boolean found = file.getTracks().stream().anyMatch(t -> matchesTrack(track, t));
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesTrack(SubtitleTrack ref, SubtitleTrack curr) {
        return ref.getLanguage().equalsIgnoreCase(curr.getLanguage()) &&
               ref.getTitle().equalsIgnoreCase(curr.getTitle());
    }
}

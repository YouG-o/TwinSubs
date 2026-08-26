package com.twinsubs.application.usecase;

import com.twinsubs.domain.model.MediaFile;
import com.twinsubs.domain.model.SubtitleTrack;

import java.util.List;
import java.util.Objects;

/**
 * Use case validating if a batch of media files shares a compatible subtitle track structure.
 */
public final class CheckCompatibilityUseCase {

    /**
     * Checks whether all media files in a selection have identical subtitle tracks.
     *
     * @param mediaFiles List of media files to check.
     * @return true if all files have compatible tracks, false otherwise.
     */
    public boolean isCompatibleBatch(List<MediaFile> mediaFiles) {
        Objects.requireNonNull(mediaFiles, "Media files list cannot be null");

        if (mediaFiles.isEmpty() || mediaFiles.size() == 1) {
            return true;
        }

        List<SubtitleTrack> referenceTracks = mediaFiles.get(0).getTracks();

        for (int i = 1; i < mediaFiles.size(); i++) {
            List<SubtitleTrack> currentTracks = mediaFiles.get(i).getTracks();
            if (!areTracksMatching(referenceTracks, currentTracks)) {
                return false;
            }
        }

        return true;
    }

    private boolean areTracksMatching(List<SubtitleTrack> refTracks, List<SubtitleTrack> currentTracks) {
        if (refTracks.size() != currentTracks.size()) {
            return false;
        }

        for (int i = 0; i < refTracks.size(); i++) {
            SubtitleTrack ref = refTracks.get(i);
            SubtitleTrack curr = currentTracks.get(i);

            // Verify index, language, and title match
            if (ref.getIndex() != curr.getIndex() ||
                !ref.getLanguage().equalsIgnoreCase(curr.getLanguage()) ||
                !ref.getTitle().equalsIgnoreCase(curr.getTitle())) {
                return false;
            }
        }

        return true;
    }
}
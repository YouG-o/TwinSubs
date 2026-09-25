package com.twinsubs.ui.component;

import com.twinsubs.domain.model.MediaFile;
import com.twinsubs.domain.model.SubtitleTrack;
import com.twinsubs.ui.service.I18nService;
import javafx.scene.control.ListCell;

import java.util.List;
import java.util.function.Predicate;

/**
 * Custom JavaFX ListCell rendering SubtitleTrack display names in ComboBoxes,
 * supporting disabled states for tracks missing in some batch files.
 */
public final class TrackListCell extends ListCell<SubtitleTrack> {

    private final List<MediaFile> mediaFiles;
    private final Predicate<SubtitleTrack> availabilityChecker;

    public TrackListCell(List<MediaFile> mediaFiles, Predicate<SubtitleTrack> availabilityChecker) {
        this.mediaFiles = mediaFiles;
        this.availabilityChecker = availabilityChecker;
    }

    @Override
    protected void updateItem(SubtitleTrack track, boolean empty) {
        super.updateItem(track, empty);
        if (empty || track == null) {
            setText(null);
            setDisable(false);
            setStyle("");
        } else {
            boolean available = availabilityChecker == null || availabilityChecker.test(track);
            if (mediaFiles != null && mediaFiles.size() > 1 && !available) {
                setText(track.getDisplayName() + " (" + I18nService.getInstance().get("track.unavailable_in_batch") + ")");
                setDisable(true);
                setStyle("-fx-text-fill: #9ca3af;");
            } else {
                setText(track.getDisplayName());
                setDisable(false);
                setStyle("");
            }
        }
    }
}
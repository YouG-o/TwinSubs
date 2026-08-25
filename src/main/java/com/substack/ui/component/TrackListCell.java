package com.substack.ui.component;

import com.substack.domain.model.SubtitleTrack;
import javafx.scene.control.ListCell;

/**
 * Custom JavaFX ListCell rendering SubtitleTrack display names in ComboBoxes.
 */
public final class TrackListCell extends ListCell<SubtitleTrack> {

    @Override
    protected void updateItem(SubtitleTrack track, boolean empty) {
        super.updateItem(track, empty);
        if (empty || track == null) {
            setText(null);
        } else {
            setText(track.getDisplayName());
        }
    }
}
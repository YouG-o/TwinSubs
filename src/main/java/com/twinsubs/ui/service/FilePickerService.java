package com.twinsubs.ui.service;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * UI Service managing native file chooser dialogs for media selection.
 */
public final class FilePickerService {

    private final I18nService i18n = I18nService.getInstance();

    /**
     * Opens a multi-selection FileChooser dialog configured for media files (.mkv, .mp4).
     *
     * @param ownerWindow Window owner for the modal dialog.
     * @return List of selected file paths, or empty list if cancelled.
     */
    public List<Path> pickMediaFiles(Window ownerWindow) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(i18n.get("app.title"));
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Media Files (*.mkv, *.mp4)", "*.mkv", "*.mp4", "*.MKV", "*.MP4"),
            new FileChooser.ExtensionFilter("MKV Files (*.mkv)", "*.mkv", "*.MKV"),
            new FileChooser.ExtensionFilter("MP4 Files (*.mp4)", "*.mp4", "*.MP4")
        );

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(ownerWindow);
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            return Collections.emptyList();
        }

        return selectedFiles.stream().map(File::toPath).toList();
    }
}
package com.twinsubs.ui.handler;

import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * UI Handler encapsulating JavaFX drag-and-drop validation and payload extraction.
 */
public final class DragAndDropHandler {

    /**
     * Handles the drag-over event, accepting copy mode if files are present.
     */
    public void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    /**
     * Handles the drag-dropped event, extracting dropped file paths and invoking callback.
     */
    public void handleDragDropped(DragEvent event, Consumer<List<Path>> onFilesDropped) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles()) {
            List<Path> droppedPaths = db.getFiles().stream().map(File::toPath).toList();
            if (onFilesDropped != null) {
                onFilesDropped.accept(droppedPaths);
            }
            success = true;
        }

        event.setDropCompleted(success);
        event.consume();
    }
}
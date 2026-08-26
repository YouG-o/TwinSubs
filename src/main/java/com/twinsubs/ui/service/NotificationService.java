package com.twinsubs.ui.service;

import javafx.scene.control.Alert;

/**
 * Service handling UI alert dialogs and user feedback notifications.
 */
public final class NotificationService {

    /**
     * Displays a success information dialog when batch processing completes.
     */
    public void showSuccessDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Displays an error alert dialog.
     */
    public void showErrorDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
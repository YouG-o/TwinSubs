package com.twinsubs.application.dto;

import java.util.Objects;

/**
 * Data transfer object reporting file processing progress.
 */
public final class ProcessingProgress {

    private final String currentFileName;
    private final String statusMessage;
    private final double progressRatio; // Value between 0.0 and 1.0

    public ProcessingProgress(String currentFileName, String statusMessage, double progressRatio) {
        this.currentFileName = Objects.requireNonNull(currentFileName, "File name cannot be null");
        this.statusMessage = Objects.requireNonNull(statusMessage, "Status message cannot be null");
        this.progressRatio = Math.max(0.0, Math.min(1.0, progressRatio));
    }

    public String getCurrentFileName() {
        return currentFileName;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public double getProgressRatio() {
        return progressRatio;
    }

    public int getPercentage() {
        return (int) Math.round(progressRatio * 100);
    }

    public String getFormattedStatus() {
        return String.format("[%d%%] %s : %s", getPercentage(), currentFileName, statusMessage);
    }
}
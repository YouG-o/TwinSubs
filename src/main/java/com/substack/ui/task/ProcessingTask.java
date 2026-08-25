package com.substack.ui.task;

import com.substack.application.dto.OutputOption;
import com.substack.application.dto.ProcessingProgress;
import com.substack.application.usecase.ProcessBilingualSubtitlesUseCase;
import com.substack.domain.model.MediaFile;
import com.substack.domain.model.PositionMode;
import com.substack.domain.model.SubtitleStyle;
import javafx.concurrent.Task;

import java.util.List;
import java.util.Objects;

/**
 * Background JavaFX Task running the subtitle processing pipeline off the UI thread.
 */
public final class ProcessingTask extends Task<Void> {

    private final ProcessBilingualSubtitlesUseCase useCase;
    private final List<MediaFile> files;
    private final int primaryTrackIndex;
    private final int secondaryTrackIndex;
    private final SubtitleStyle primaryStyle;
    private final SubtitleStyle secondaryStyle;
    private final PositionMode positionMode;
    private final OutputOption outputOption;

    public ProcessingTask(ProcessBilingualSubtitlesUseCase useCase,
                          List<MediaFile> files,
                          int primaryTrackIndex,
                          int secondaryTrackIndex,
                          SubtitleStyle primaryStyle,
                          SubtitleStyle secondaryStyle,
                          PositionMode positionMode,
                          OutputOption outputOption) {
        this.useCase = Objects.requireNonNull(useCase);
        this.files = Objects.requireNonNull(files);
        this.primaryTrackIndex = primaryTrackIndex;
        this.secondaryTrackIndex = secondaryTrackIndex;
        this.primaryStyle = Objects.requireNonNull(primaryStyle);
        this.secondaryStyle = Objects.requireNonNull(secondaryStyle);
        this.positionMode = Objects.requireNonNull(positionMode);
        this.outputOption = Objects.requireNonNull(outputOption);
    }

    @Override
    protected Void call() throws Exception {
        useCase.execute(
            files,
            primaryTrackIndex,
            secondaryTrackIndex,
            primaryStyle,
            secondaryStyle,
            positionMode,
            outputOption,
            this::onProgressUpdate
        );
        return null;
    }

    private void onProgressUpdate(ProcessingProgress progress) {
        updateProgress(progress.getProgressRatio(), 1.0);
        updateMessage(progress.getCurrentFileName() + " : " + progress.getStatusMessage());
    }
}
package com.twinsubs.ui.task;

import com.twinsubs.application.dto.OutputOption;
import com.twinsubs.application.dto.ProcessingProgress;
import com.twinsubs.application.usecase.ProcessBilingualSubtitlesUseCase;
import com.twinsubs.domain.model.MediaFile;
import com.twinsubs.domain.model.PositionMode;
import com.twinsubs.domain.model.SubtitleLayout;
import com.twinsubs.domain.model.SubtitleStyle;
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
    private final SubtitleLayout layout;
    private final OutputOption outputOption;

    public ProcessingTask(ProcessBilingualSubtitlesUseCase useCase,
                          List<MediaFile> files,
                          int primaryTrackIndex,
                          int secondaryTrackIndex,
                          SubtitleStyle primaryStyle,
                          SubtitleStyle secondaryStyle,
                          PositionMode positionMode,
                          OutputOption outputOption) {
                this(useCase, files, primaryTrackIndex, secondaryTrackIndex, primaryStyle, secondaryStyle,
                    SubtitleLayout.defaultLayout(positionMode), outputOption);
                }

                public ProcessingTask(ProcessBilingualSubtitlesUseCase useCase,
                          List<MediaFile> files,
                          int primaryTrackIndex,
                          int secondaryTrackIndex,
                          SubtitleStyle primaryStyle,
                          SubtitleStyle secondaryStyle,
                          SubtitleLayout layout,
                          OutputOption outputOption) {
        this.useCase = Objects.requireNonNull(useCase);
        this.files = Objects.requireNonNull(files);
        this.primaryTrackIndex = primaryTrackIndex;
        this.secondaryTrackIndex = secondaryTrackIndex;
        this.primaryStyle = Objects.requireNonNull(primaryStyle);
        this.secondaryStyle = Objects.requireNonNull(secondaryStyle);
        this.layout = Objects.requireNonNull(layout);
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
            layout,
            outputOption,
            this::onProgressUpdate
        );
        return null;
    }

    private void onProgressUpdate(ProcessingProgress progress) {
        updateProgress(progress.getProgressRatio(), 1.0);
        updateMessage(progress.getFormattedStatus());
    }
}
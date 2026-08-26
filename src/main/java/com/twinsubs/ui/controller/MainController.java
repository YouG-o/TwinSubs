package com.twinsubs.ui.controller;

import com.twinsubs.application.dto.OutputOption;
import com.twinsubs.application.usecase.CheckCompatibilityUseCase;
import com.twinsubs.application.usecase.ProcessBilingualSubtitlesUseCase;
import com.twinsubs.domain.model.MediaFile;
import com.twinsubs.domain.model.PositionMode;
import com.twinsubs.domain.model.SubtitleStyle;
import com.twinsubs.domain.model.SubtitleTrack;
import com.twinsubs.domain.service.TemporalOverlapMatcher;
import com.twinsubs.infrastructure.ffmpeg.ProcessFfmpegService;
import com.twinsubs.infrastructure.formatter.AssFormatter;
import com.twinsubs.infrastructure.io.FileScanner;
import com.twinsubs.infrastructure.parser.SrtParser;
import com.twinsubs.ui.component.TrackListCell;
import com.twinsubs.ui.service.I18nService;
import com.twinsubs.ui.service.NotificationService;
import com.twinsubs.ui.task.ProcessingTask;
import com.twinsubs.ui.util.ColorUtils;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller managing user interactions and binding UI components to application use cases.
 */
public final class MainController {

    @FXML private StackPane dropZone;
    @FXML private Label lblFileSummary;
    @FXML private ComboBox<SubtitleTrack> comboPrimaryTrack;
    @FXML private ComboBox<SubtitleTrack> comboSecondaryTrack;

    @FXML private TextField txtPrimaryFont;
    @FXML private Spinner<Integer> spnPrimarySize;
    @FXML private ColorPicker cpPrimaryColor;
    @FXML private CheckBox chkPrimaryBold;
    @FXML private CheckBox chkPrimaryItalic;

    @FXML private TextField txtSecondaryFont;
    @FXML private Spinner<Integer> spnSecondarySize;
    @FXML private ColorPicker cpSecondaryColor;
    @FXML private CheckBox chkSecondaryBold;
    @FXML private CheckBox chkSecondaryItalic;

    @FXML private ComboBox<PositionMode> comboPositionMode;
    @FXML private ComboBox<OutputOption> comboOutputOption;

    @FXML private ProgressBar progressBar;
    @FXML private Label lblStatus;
    @FXML private Button btnStart;

    private final FileScanner fileScanner = new FileScanner();
    private final ProcessFfmpegService ffmpegService = new ProcessFfmpegService();
    private final CheckCompatibilityUseCase compatibilityUseCase = new CheckCompatibilityUseCase();
    private final NotificationService notificationService = new NotificationService();
    private final I18nService i18n = I18nService.getInstance();
    private final ProcessBilingualSubtitlesUseCase processUseCase = new ProcessBilingualSubtitlesUseCase(
        ffmpegService, new SrtParser(), new TemporalOverlapMatcher(), new AssFormatter()
    );

    private final List<MediaFile> loadedMediaFiles = new ArrayList<>();

    @FXML
    public void initialize() {
        cpPrimaryColor.setValue(Color.web("#00FFFF"));
        cpSecondaryColor.setValue(Color.WHITE);

        comboPositionMode.getItems().setAll(PositionMode.values());
        comboPositionMode.setValue(PositionMode.BOTH_BOTTOM);

        comboOutputOption.getItems().setAll(OutputOption.values());
        comboOutputOption.setValue(OutputOption.EMBED_IN_MKV);

        comboPrimaryTrack.setCellFactory(p -> new TrackListCell());
        comboPrimaryTrack.setButtonCell(new TrackListCell());
        comboSecondaryTrack.setCellFactory(p -> new TrackListCell());
        comboSecondaryTrack.setButtonCell(new TrackListCell());
    }

    @FXML
    public void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    @FXML
    public void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            List<Path> droppedPaths = db.getFiles().stream().map(File::toPath).toList();
            loadFilesAsync(droppedPaths);
        }
        event.setDropCompleted(db.hasFiles());
        event.consume();
    }

    private void loadFilesAsync(List<Path> inputPaths) {
        btnStart.setDisable(true);
        lblStatus.setText(i18n.get("status.analyzing"));

        Task<List<MediaFile>> scanTask = new Task<>() {
            @Override
            protected List<MediaFile> call() throws Exception {
                List<Path> scanned = fileScanner.scanPaths(inputPaths);
                List<MediaFile> mediaFiles = new ArrayList<>();
                for (Path path : scanned) {
                    List<SubtitleTrack> tracks = ffmpegService.inspectSubtitleTracks(path);
                    mediaFiles.add(new MediaFile(path, tracks));
                }
                return mediaFiles;
            }
        };

        scanTask.setOnSucceeded(e -> onFilesLoaded(scanTask.getValue()));
        scanTask.setOnFailed(e -> notificationService.showErrorDialog(
            i18n.get("dialog.error.title"), scanTask.getException().getMessage()
        ));

        new Thread(scanTask).start();
    }

    private void onFilesLoaded(List<MediaFile> mediaFiles) {
        loadedMediaFiles.clear();
        loadedMediaFiles.addAll(mediaFiles);

        if (loadedMediaFiles.isEmpty()) {
            lblFileSummary.setText(i18n.get("dropzone.summary.no_valid"));
            return;
        }

        if (!compatibilityUseCase.isCompatibleBatch(loadedMediaFiles)) {
            lblFileSummary.setText(i18n.get("dropzone.summary.incompatible"));
            notificationService.showErrorDialog(
                i18n.get("dialog.compatibility.title"),
                i18n.get("dialog.compatibility.content")
            );
            return;
        }

        lblFileSummary.setText(i18n.get("dropzone.summary.ready", loadedMediaFiles.size()));
        List<SubtitleTrack> availableTracks = loadedMediaFiles.get(0).getTracks();

        comboPrimaryTrack.getItems().setAll(availableTracks);
        comboSecondaryTrack.getItems().setAll(availableTracks);

        if (!availableTracks.isEmpty()) {
            comboPrimaryTrack.setValue(availableTracks.get(0));
            comboSecondaryTrack.setValue(availableTracks.size() > 1 ? availableTracks.get(1) : availableTracks.get(0));
        }

        btnStart.setDisable(false);
        lblStatus.setText(i18n.get("status.ready"));
    }

    @FXML
    public void handleStartProcessing() {
        SubtitleTrack primaryTrack = comboPrimaryTrack.getValue();
        SubtitleTrack secondaryTrack = comboSecondaryTrack.getValue();

        if (primaryTrack == null || secondaryTrack == null) {
            notificationService.showErrorDialog(
                i18n.get("dialog.missing_tracks.title"),
                i18n.get("dialog.missing_tracks.content")
            );
            return;
        }

        SubtitleStyle primaryStyle = new SubtitleStyle(
            txtPrimaryFont.getText(), spnPrimarySize.getValue(),
            ColorUtils.toHexString(cpPrimaryColor.getValue()),
            chkPrimaryBold.isSelected(), chkPrimaryItalic.isSelected()
        );

        SubtitleStyle secondaryStyle = new SubtitleStyle(
            txtSecondaryFont.getText(), spnSecondarySize.getValue(),
            ColorUtils.toHexString(cpSecondaryColor.getValue()),
            chkSecondaryBold.isSelected(), chkSecondaryItalic.isSelected()
        );

        btnStart.setDisable(true);

        ProcessingTask task = new ProcessingTask(
            processUseCase, loadedMediaFiles, primaryTrack.getIndex(), secondaryTrack.getIndex(),
            primaryStyle, secondaryStyle, comboPositionMode.getValue(), comboOutputOption.getValue()
        );

        progressBar.progressProperty().bind(task.progressProperty());
        lblStatus.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            unbindProgress();
            progressBar.setProgress(1.0);
            lblStatus.setText(i18n.get("status.done"));
            btnStart.setDisable(false);

            notificationService.showSuccessDialog(
                i18n.get("dialog.success.title"),
                i18n.get("dialog.success.header"),
                i18n.get("dialog.success.content")
            );
        });

        task.setOnFailed(e -> {
            unbindProgress();
            progressBar.setProgress(0.0);
            notificationService.showErrorDialog(
                i18n.get("dialog.error.title"),
                task.getException().getMessage()
            );
            btnStart.setDisable(false);
        });

        new Thread(task).start();
    }

    private void unbindProgress() {
        progressBar.progressProperty().unbind();
        lblStatus.textProperty().unbind();
    }
}
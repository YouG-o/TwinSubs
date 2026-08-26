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
import com.twinsubs.ui.handler.DragAndDropHandler;
import com.twinsubs.ui.service.FilePickerService;
import com.twinsubs.ui.service.I18nService;
import com.twinsubs.ui.service.NotificationService;
import com.twinsubs.ui.service.SubtitlePreviewManager;
import com.twinsubs.ui.task.ProcessingTask;
import com.twinsubs.ui.util.ColorUtils;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller managing user interactions and binding UI components to application use cases.
 */
public final class MainController {

    @FXML private VBox vboxInitialDrop;
    @FXML private HBox hboxCompactSummary;
    @FXML private Label lblCompactFileSummary;
    @FXML private VBox vboxConfiguration;

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

    @FXML private ImageView imgPreviewBackground;
    @FXML private VBox vboxPreviewTop;
    @FXML private VBox vboxPreviewBottom;

    @FXML private ProgressBar progressBar;
    @FXML private Label lblStatus;
    @FXML private Button btnStart;

    private final FileScanner fileScanner = new FileScanner();
    private final ProcessFfmpegService ffmpegService = new ProcessFfmpegService();
    private final CheckCompatibilityUseCase compatibilityUseCase = new CheckCompatibilityUseCase();
    private final NotificationService notificationService = new NotificationService();
    private final FilePickerService filePickerService = new FilePickerService();
    private final DragAndDropHandler dragAndDropHandler = new DragAndDropHandler();
    private final I18nService i18n = I18nService.getInstance();
    private final ProcessBilingualSubtitlesUseCase processUseCase = new ProcessBilingualSubtitlesUseCase(
        ffmpegService, new SrtParser(), new TemporalOverlapMatcher(), new AssFormatter()
    );

    private SubtitlePreviewManager previewManager;

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

        previewManager = new SubtitlePreviewManager(imgPreviewBackground, vboxPreviewTop, vboxPreviewBottom);
        attachPreviewListeners();
        updatePreview();
    }

    @FXML
    public void handleDragOver(DragEvent event) {
        dragAndDropHandler.handleDragOver(event);
    }
    @FXML
    public void handleDragDropped(DragEvent event) {
        dragAndDropHandler.handleDragDropped(event, this::loadFilesAsync);
    }
    @FXML
    public void handleZoneClick(MouseEvent event) {
        List<Path> selectedPaths = filePickerService.pickMediaFiles(dropZone.getScene().getWindow());
        if (!selectedPaths.isEmpty()) {
            loadFilesAsync(selectedPaths);
        }
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
            setViewState(false);
            return;
        }

        if (!compatibilityUseCase.isCompatibleBatch(loadedMediaFiles)) {
            lblFileSummary.setText(i18n.get("dropzone.summary.incompatible"));
            setViewState(false);
            notificationService.showErrorDialog(
                i18n.get("dialog.compatibility.title"),
                i18n.get("dialog.compatibility.content")
            );
            return;
        }

        // Format compact file summary text
        String firstFileName = loadedMediaFiles.get(0).getFileName();
        if (loadedMediaFiles.size() == 1) {
            lblCompactFileSummary.setText(i18n.get("file.summary.single", firstFileName));
        } else {
            lblCompactFileSummary.setText(i18n.get("file.summary.batch", firstFileName, loadedMediaFiles.size() - 1));
        }

        List<SubtitleTrack> availableTracks = loadedMediaFiles.get(0).getTracks();
        comboPrimaryTrack.getItems().setAll(availableTracks);
        comboSecondaryTrack.getItems().setAll(availableTracks);

        if (!availableTracks.isEmpty()) {
            comboPrimaryTrack.setValue(availableTracks.get(0));
            comboSecondaryTrack.setValue(availableTracks.size() > 1 ? availableTracks.get(1) : availableTracks.get(0));
        }

        setViewState(true);
        btnStart.setDisable(false);
        lblStatus.setText(i18n.get("status.ready"));
    }

    @FXML
    public void handleChangeFiles() {
        loadedMediaFiles.clear();
        lblFileSummary.setText(i18n.get("dropzone.summary.none"));
        btnStart.setDisable(true);
        setViewState(false);
    }

    private void setViewState(boolean filesLoaded) {
        vboxInitialDrop.setVisible(!filesLoaded);
        vboxInitialDrop.setManaged(!filesLoaded);

        hboxCompactSummary.setVisible(filesLoaded);
        hboxCompactSummary.setManaged(filesLoaded);

        vboxConfiguration.setVisible(filesLoaded);
        vboxConfiguration.setManaged(filesLoaded);
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

    private void attachPreviewListeners() {
        txtPrimaryFont.textProperty().addListener((obs, o, n) -> updatePreview());
        spnPrimarySize.valueProperty().addListener((obs, o, n) -> updatePreview());
        cpPrimaryColor.valueProperty().addListener((obs, o, n) -> updatePreview());
        chkPrimaryBold.selectedProperty().addListener((obs, o, n) -> updatePreview());
        chkPrimaryItalic.selectedProperty().addListener((obs, o, n) -> updatePreview());

        txtSecondaryFont.textProperty().addListener((obs, o, n) -> updatePreview());
        spnSecondarySize.valueProperty().addListener((obs, o, n) -> updatePreview());
        cpSecondaryColor.valueProperty().addListener((obs, o, n) -> updatePreview());
        chkSecondaryBold.selectedProperty().addListener((obs, o, n) -> updatePreview());
        chkSecondaryItalic.selectedProperty().addListener((obs, o, n) -> updatePreview());

        comboPositionMode.valueProperty().addListener((obs, o, n) -> updatePreview());
    }

    private void updatePreview() {
        if (previewManager == null) {
            return;
        }

        SubtitleStyle primaryStyle = new SubtitleStyle(
            txtPrimaryFont.getText(),
            spnPrimarySize.getValue() != null ? spnPrimarySize.getValue() : 50,
            ColorUtils.toHexString(cpPrimaryColor.getValue()),
            chkPrimaryBold.isSelected(),
            chkPrimaryItalic.isSelected()
        );

        SubtitleStyle secondaryStyle = new SubtitleStyle(
            txtSecondaryFont.getText(),
            spnSecondarySize.getValue() != null ? spnSecondarySize.getValue() : 38,
            ColorUtils.toHexString(cpSecondaryColor.getValue()),
            chkSecondaryBold.isSelected(),
            chkSecondaryItalic.isSelected()
        );

        previewManager.updatePreview(primaryStyle, secondaryStyle, comboPositionMode.getValue());
    }
}


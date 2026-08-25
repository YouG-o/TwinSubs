package com.substack.ui.controller;

import com.substack.application.dto.OutputOption;
import com.substack.application.usecase.CheckCompatibilityUseCase;
import com.substack.application.usecase.ProcessBilingualSubtitlesUseCase;
import com.substack.domain.model.MediaFile;
import com.substack.domain.model.PositionMode;
import com.substack.domain.model.SubtitleStyle;
import com.substack.domain.model.SubtitleTrack;
import com.substack.domain.service.TemporalOverlapMatcher;
import com.substack.infrastructure.ffmpeg.ProcessFfmpegService;
import com.substack.infrastructure.formatter.AssFormatter;
import com.substack.infrastructure.io.FileScanner;
import com.substack.infrastructure.parser.SrtParser;
import com.substack.ui.component.TrackListCell;
import com.substack.ui.service.NotificationService;
import com.substack.ui.task.ProcessingTask;
import com.substack.ui.util.ColorUtils;

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
        lblStatus.setText("Analyse des fichiers...");

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
        scanTask.setOnFailed(e -> notificationService.showErrorDialog("Erreur d'analyse", scanTask.getException().getMessage()));

        new Thread(scanTask).start();
    }

    private void onFilesLoaded(List<MediaFile> mediaFiles) {
        loadedMediaFiles.clear();
        loadedMediaFiles.addAll(mediaFiles);

        if (loadedMediaFiles.isEmpty()) {
            lblFileSummary.setText("Aucun fichier compatible trouvé.");
            return;
        }

        if (!compatibilityUseCase.isCompatibleBatch(loadedMediaFiles)) {
            lblFileSummary.setText("Fichiers incompatibles.");
            notificationService.showErrorDialog("Erreur de compatibilité",
                "Les fichiers sélectionnés ne possèdent pas une structure de sous-titres compatible. Veuillez traiter ces fichiers individuellement.");
            return;
        }

        lblFileSummary.setText(loadedMediaFiles.size() + " fichier(s) prêt(s)");
        List<SubtitleTrack> availableTracks = loadedMediaFiles.get(0).getTracks();

        comboPrimaryTrack.getItems().setAll(availableTracks);
        comboSecondaryTrack.getItems().setAll(availableTracks);

        if (!availableTracks.isEmpty()) {
            comboPrimaryTrack.setValue(availableTracks.get(0));
            comboSecondaryTrack.setValue(availableTracks.size() > 1 ? availableTracks.get(1) : availableTracks.get(0));
        }

        btnStart.setDisable(false);
        lblStatus.setText("Prêt.");
    }

    @FXML
    public void handleStartProcessing() {
        SubtitleTrack primaryTrack = comboPrimaryTrack.getValue();
        SubtitleTrack secondaryTrack = comboSecondaryTrack.getValue();

        if (primaryTrack == null || secondaryTrack == null) {
            notificationService.showErrorDialog("Pistes manquantes", "Veuillez sélectionner les deux pistes de sous-titres.");
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
            lblStatus.setText("Traitement terminé avec succès !");
            btnStart.setDisable(false);

            notificationService.showSuccessDialog(
                "Traitement Terminé",
                "Sous-titres bilingues générés !",
                "Le traitement est terminé. Les fichiers générés se trouvent dans le dossier d'origine de vos vidéos."
            );
        });

        task.setOnFailed(e -> {
            unbindProgress();
            progressBar.setProgress(0.0);
            notificationService.showErrorDialog("Erreur de traitement", task.getException().getMessage());
            btnStart.setDisable(false);
        });

        new Thread(task).start();
    }

    private void unbindProgress() {
        progressBar.progressProperty().unbind();
        lblStatus.textProperty().unbind();
    }

    private void showErrorMessage(String title, String message) {
        lblStatus.setText("Erreur : " + title);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
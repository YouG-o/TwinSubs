package com.twinsubs.ui.service;

import com.twinsubs.domain.model.PositionMode;
import com.twinsubs.domain.model.SubtitleLayout;
import com.twinsubs.domain.model.SubtitleStyle;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import java.io.InputStream;

/**
 * Service responsible for managing and updating the live video preview canvas with styled subtitles.
 */
public final class SubtitlePreviewManager {

    private static final double SCALE_FACTOR = 0.24; // Scales ASS font size (1080p video canvas) to preview canvas height
    private static final String PREVIEW_IMAGE_PATH = "/images/preview_background.png";

    private final ImageView imgPreviewBackground;
    private final VBox vboxPreviewTop;
    private final VBox vboxPreviewBottom;
    private final I18nService i18n = I18nService.getInstance();

    public SubtitlePreviewManager(ImageView imgPreviewBackground, VBox vboxPreviewTop, VBox vboxPreviewBottom) {
        this.imgPreviewBackground = imgPreviewBackground;
        this.vboxPreviewTop = vboxPreviewTop;
        this.vboxPreviewBottom = vboxPreviewBottom;
        loadBackgroundImage();
    }

    private void loadBackgroundImage() {
        try (InputStream is = getClass().getResourceAsStream(PREVIEW_IMAGE_PATH)) {
            if (is != null) {
                imgPreviewBackground.setImage(new Image(is));
            }
        } catch (Exception ignored) {
            // Fallback to CSS dark video container background if background image is missing
        }
    }

    public void updatePreview(SubtitleStyle primaryStyle, SubtitleStyle secondaryStyle, PositionMode positionMode) {
        updatePreview(primaryStyle, secondaryStyle, SubtitleLayout.defaultLayout(positionMode));
    }

    public void updatePreview(SubtitleStyle primaryStyle, SubtitleStyle secondaryStyle, SubtitleLayout layout) {
        vboxPreviewTop.getChildren().clear();
        vboxPreviewBottom.getChildren().clear();

        Label lblPrimary = createPreviewLabel(i18n.get("preview.text.primary"), primaryStyle);
        Label lblSecondary = createPreviewLabel(i18n.get("preview.text.secondary"), secondaryStyle);

        PositionMode mode = layout != null ? layout.positionMode() : PositionMode.BOTH_BOTTOM;
        boolean primaryFirst = layout != null && layout.isFirstTrackPrimary();
        Label firstLabel = primaryFirst ? lblPrimary : lblSecondary;
        Label secondLabel = primaryFirst ? lblSecondary : lblPrimary;

        switch (mode) {
            case BOTH_BOTTOM -> vboxPreviewBottom.getChildren().addAll(firstLabel, secondLabel);
            case BOTH_TOP -> vboxPreviewTop.getChildren().addAll(firstLabel, secondLabel);
            case TOP_AND_BOTTOM -> {
                vboxPreviewTop.getChildren().add(firstLabel);
                vboxPreviewBottom.getChildren().add(secondLabel);
            }
        }
    }

    private Label createPreviewLabel(String text, SubtitleStyle style) {
        Label label = new Label(text);
        label.getStyleClass().add("subtitle-preview-text");

        double scaledSize = Math.max(9.0, style.getFontSize() * SCALE_FACTOR);
        FontWeight weight = style.isBold() ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture posture = style.isItalic() ? FontPosture.ITALIC : FontPosture.REGULAR;

        label.setFont(Font.font(style.getFontName(), weight, posture, scaledSize));

        String colorHex = style.getHexColor();
        String hexColor = colorHex.startsWith("#") ? colorHex : "#" + colorHex;
        label.setStyle("-fx-text-fill: " + hexColor + ";");

        return label;
    }
}
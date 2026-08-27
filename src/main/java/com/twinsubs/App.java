package com.twinsubs;

import com.twinsubs.ui.service.I18nService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        I18nService i18n = I18nService.getInstance();
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/MainView.fxml"),
            i18n.getBundle()
        );
        Parent root = loader.load();

        // Initial window dimensions set to comfortably fit the 2-column layout without clipping
        Scene scene = new Scene(root, 940, 720);

        primaryStage.setTitle("TwinSubs - Bilingual Subtitle Generator");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(680);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
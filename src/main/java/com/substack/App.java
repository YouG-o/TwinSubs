package com.substack;

import com.substack.ui.service.I18nService;
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

        Scene scene = new Scene(root, 800, 680);

        primaryStage.setTitle("SubStack - Bilingual Subtitle Generator");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(750);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
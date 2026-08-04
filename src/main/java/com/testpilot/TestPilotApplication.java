package com.testpilot;

import com.testpilot.config.AppBootstrap;
import com.testpilot.controller.AppController;
import com.testpilot.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public final class TestPilotApplication extends Application {
    private AppController controller;

    @Override
    public void start(Stage stage) {
        controller = AppBootstrap.create(java.nio.file.Path.of("."));
        MainView view = new MainView(controller, stage);
        Scene scene = new Scene(view, 1480, 920);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/testpilot.css")).toExternalForm());
        stage.setTitle("TestPilot Studio");
        stage.setMinWidth(1180);
        stage.setMinHeight(720);
        try {
            stage.getIcons().add(new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/assets/testpilot-logo.png"))));
        } catch (RuntimeException ignored) {
            // Ung dung van chay neu icon bi thieu.
        }
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (controller != null) controller.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

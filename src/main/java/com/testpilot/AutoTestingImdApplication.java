package com.testpilot;

import com.testpilot.config.AppBootstrap;
import com.testpilot.controller.AppController;
import com.testpilot.ui.MainView;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.Objects;

public final class AutoTestingImdApplication extends Application {
    private AppController controller;

    @Override
    public void start(Stage stage) {
        controller = AppBootstrap.create(java.nio.file.Path.of("."));
        MainView view = new MainView(controller, stage);
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.max(800, bounds.getWidth() - 80);
        double height = Math.max(600, bounds.getHeight() - 80);
        Scene scene = new Scene(view, width, height);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/testpilot.css")).toExternalForm());
        stage.setTitle("AUTO TESTING IMD");
        stage.setMinWidth(Math.min(1180, Math.max(800, bounds.getWidth() - 80)));
        stage.setMinHeight(Math.min(720, Math.max(600, bounds.getHeight() - 80)));
        stage.setResizable(true);
        try {
            stage.getIcons().add(new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/assets/testpilot-logo.png"))));
        } catch (RuntimeException ignored) {
            // Ung dung van chay neu icon bi thieu.
        }
        stage.setScene(scene);
        stage.setX(bounds.getMinX() + 20);
        stage.setY(bounds.getMinY() + 20);
        stage.show();
        stage.setMaximized(true);
    }

    @Override
    public void stop() {
        if (controller != null) controller.close();
    }
}

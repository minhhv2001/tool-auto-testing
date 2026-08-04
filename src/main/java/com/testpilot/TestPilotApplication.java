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

public final class TestPilotApplication extends Application {
    private AppController controller;

    @Override
    public void start(Stage stage) {
        controller = AppBootstrap.create(java.nio.file.Path.of("."));
        MainView view = new MainView(controller, stage);
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1480, Math.max(800, bounds.getWidth() - 40));
        double height = Math.min(920, Math.max(600, bounds.getHeight() - 40));
        Scene scene = new Scene(view, width, height);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/testpilot.css")).toExternalForm());
        stage.setTitle("TestPilot Studio");
        stage.setMinWidth(Math.min(1180, width));
        stage.setMinHeight(Math.min(720, height));
        try {
            stage.getIcons().add(new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/assets/testpilot-logo.png"))));
        } catch (RuntimeException ignored) {
            // Ung dung van chay neu icon bi thieu.
        }
        stage.setScene(scene);
        stage.setX(bounds.getMinX() + (bounds.getWidth() - width) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - height) / 2);
        stage.show();
    }

    @Override
    public void stop() {
        if (controller != null) controller.close();
    }
}

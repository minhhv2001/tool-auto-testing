package com.testpilot.ui.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class KpiCard extends VBox {
    private final Label value = new Label("0");
    private final Label caption = new Label();

    public KpiCard(String icon, String title, String accentClass) {
        getStyleClass().addAll("card", "kpi-card", accentClass);
        setSpacing(9);
        setPadding(new Insets(20));
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("kpi-icon");
        value.getStyleClass().add("kpi-value");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kpi-title");
        caption.getStyleClass().add("muted");
        getChildren().addAll(iconLabel, value, titleLabel, caption);
    }

    public void setValue(String value) {
        this.value.setText(value);
    }

    public void setCaption(String caption) {
        this.caption.setText(caption);
    }
}

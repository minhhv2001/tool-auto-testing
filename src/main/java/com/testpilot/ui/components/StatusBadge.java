package com.testpilot.ui.components;

import com.testpilot.model.enums.RunStatus;
import javafx.scene.control.Label;

public final class StatusBadge extends Label {
    public StatusBadge() {
        getStyleClass().add("status-badge");
    }

    public void setStatus(RunStatus status) {
        setText(textFor(status));
        getStyleClass().removeAll("status-queued", "status-running", "status-passed", "status-failed", "status-cancelled");
        getStyleClass().add("status-" + status.name().toLowerCase());
    }

    private static String textFor(RunStatus status) {
        switch (status) {
            case QUEUED:
                return "CHO CHAY";
            case RUNNING:
                return "DANG CHAY";
            case PASSED:
                return "PASS";
            case FAILED:
                return "FAIL";
            case CANCELLED:
                return "DA DUNG";
            default:
                throw new IllegalArgumentException("Trang thai khong hop le: " + status);
        }
    }
}

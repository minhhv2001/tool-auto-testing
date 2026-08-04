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
                return "ĐANG CHỜ";
            case RUNNING:
                return "ĐANG CHẠY";
            case PASSED:
                return "ĐẠT";
            case FAILED:
                return "KHÔNG ĐẠT";
            case CANCELLED:
                return "ĐÃ DỪNG";
            default:
                throw new IllegalArgumentException("Trạng thái không hợp lệ: " + status);
        }
    }
}

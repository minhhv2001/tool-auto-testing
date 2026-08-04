package com.testpilot.model.enums;

import java.util.Locale;

public enum ActionType {
    GOTO,
    CLICK,
    FILL,
    PRESS,
    SELECT,
    CHECK,
    UNCHECK,
    UPLOAD,
    WAIT,
    EXPECT_TEXT,
    EXPECT_VISIBLE,
    EXPECT_HIDDEN,
    EXPECT_URL,
    EXPECT_ROWS_CONTAIN,
    SCREENSHOT;

    public static ActionType fromCell(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Thao tác không được để trống");
        }
        String normalized = value.trim()
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "EXPECTTEXT":
                return EXPECT_TEXT;
            case "EXPECTVISIBLE":
                return EXPECT_VISIBLE;
            case "EXPECTHIDDEN":
                return EXPECT_HIDDEN;
            case "EXPECTURL":
                return EXPECT_URL;
            case "EXPECTROWSCONTAIN":
            case "EXPECTROWS":
            case "ROWS_CONTAIN":
                return EXPECT_ROWS_CONTAIN;
            default:
                return ActionType.valueOf(normalized);
        }
    }
}

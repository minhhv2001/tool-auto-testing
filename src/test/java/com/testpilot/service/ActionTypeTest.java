package com.testpilot.service;

import com.testpilot.model.enums.ActionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActionTypeTest {
    @Test
    void parsesCamelCaseExcelKeywords() {
        assertEquals(ActionType.EXPECT_TEXT, ActionType.fromCell("expectText"));
        assertEquals(ActionType.EXPECT_ROWS_CONTAIN, ActionType.fromCell("expectRowsContain"));
    }
}

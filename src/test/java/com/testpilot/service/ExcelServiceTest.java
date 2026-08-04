package com.testpilot.service;

import com.testpilot.model.enums.ActionType;
import com.testpilot.model.response.ImportResult;
import com.testpilot.service.impl.ExcelServiceImpl;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelServiceTest {
    @Test
    void importsTheDeliveredAutomationTemplate() {
        ImportResult result = new ExcelServiceImpl().importAutomationSteps(
                Path.of("sample-data/TestPilot_Automation_Template.xlsx"));
        assertEquals(2, result.testCaseCount());
        assertEquals(8, result.steps().size());
        assertEquals(6, result.steps().stream().filter(step -> step.enabled()).count());
        assertEquals(1, result.steps().stream()
                .filter(step -> step.action() == ActionType.EXPECT_ROWS_CONTAIN).count());
    }
}

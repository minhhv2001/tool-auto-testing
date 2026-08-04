package com.testpilot.service;

import com.testpilot.model.response.ImportResult;

import java.nio.file.Path;

public interface ExcelService {
    ImportResult importAutomationSteps(Path file);

    default ImportResult importAutomationSteps(Path file, String sheetName) {
        return importAutomationSteps(file);
    }

    default ImportResult importAutomationSteps(Path file, String sheetName, int startRow, int startColumn) {
        return importAutomationSteps(file, sheetName);
    }
}

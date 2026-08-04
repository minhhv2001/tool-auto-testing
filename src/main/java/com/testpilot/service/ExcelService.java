package com.testpilot.service;

import com.testpilot.model.response.ImportResult;

import java.nio.file.Path;

public interface ExcelService {
    ImportResult importAutomationSteps(Path file);
}

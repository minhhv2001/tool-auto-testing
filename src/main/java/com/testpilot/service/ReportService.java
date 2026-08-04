package com.testpilot.service;

import com.testpilot.model.entity.TestRun;
import com.testpilot.model.response.StepResult;

import java.nio.file.Path;
import java.util.List;

public interface ReportService {
    Path writeExcelReport(TestRun run, List<StepResult> results, Path outputFile);

    Path writeLog(TestRun run, List<StepResult> results, Path outputFile);
}

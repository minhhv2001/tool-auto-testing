package com.testpilot.model.response;

import com.testpilot.model.entity.TestStep;

import java.util.List;

public final class ImportResult {
    private final List<TestStep> steps;
    private final List<String> warnings;

    public ImportResult(List<TestStep> steps, List<String> warnings) {
        this.steps = steps;
        this.warnings = warnings;
    }

    public List<TestStep> steps() {
        return steps;
    }

    public List<String> warnings() {
        return warnings;
    }

    public int testCaseCount() {
        return (int) steps.stream().map(TestStep::testCaseId).distinct().count();
    }
}

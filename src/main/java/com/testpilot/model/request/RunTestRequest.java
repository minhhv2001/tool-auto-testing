package com.testpilot.model.request;

import com.testpilot.model.entity.TestFeature;
import com.testpilot.model.entity.TestProject;

import java.nio.file.Path;
import java.util.Map;

public final class RunTestRequest {
    private final TestProject project;
    private final TestFeature feature;
    private final Path excelFile;
    private final boolean headless;
    private final Map<String, String> sessionSecrets;

    public RunTestRequest(TestProject project, TestFeature feature, Path excelFile,
                          boolean headless, Map<String, String> sessionSecrets) {
        this.project = project;
        this.feature = feature;
        this.excelFile = excelFile;
        this.headless = headless;
        this.sessionSecrets = sessionSecrets;
    }

    public TestProject project() {
        return project;
    }

    public TestFeature feature() {
        return feature;
    }

    public Path excelFile() {
        return excelFile;
    }

    public boolean headless() {
        return headless;
    }

    public Map<String, String> sessionSecrets() {
        return sessionSecrets;
    }
}

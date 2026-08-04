package com.testpilot.model.entity;

import com.testpilot.model.enums.RunStatus;

import java.nio.file.Path;
import java.time.LocalDateTime;

public final class TestRun {
    private final String id;
    private final long projectId;
    private final long featureId;
    private final String projectName;
    private final String featureName;
    private final String sourceFile;
    private final RunStatus status;
    private final int progress;
    private final int passedSteps;
    private final int failedSteps;
    private final String currentStep;
    private final String errorMessage;
    private final LocalDateTime startedAt;
    private final LocalDateTime finishedAt;
    private final Path artifactDirectory;

    public TestRun(String id, long projectId, long featureId, String projectName, String featureName,
                   String sourceFile, RunStatus status, int progress, int passedSteps, int failedSteps,
                   String currentStep, String errorMessage, LocalDateTime startedAt,
                   LocalDateTime finishedAt, Path artifactDirectory) {
        this.id = id;
        this.projectId = projectId;
        this.featureId = featureId;
        this.projectName = projectName;
        this.featureName = featureName;
        this.sourceFile = sourceFile;
        this.status = status;
        this.progress = progress;
        this.passedSteps = passedSteps;
        this.failedSteps = failedSteps;
        this.currentStep = currentStep;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.artifactDirectory = artifactDirectory;
    }

    public String id() {
        return id;
    }

    public long projectId() {
        return projectId;
    }

    public long featureId() {
        return featureId;
    }

    public String projectName() {
        return projectName;
    }

    public String featureName() {
        return featureName;
    }

    public String sourceFile() {
        return sourceFile;
    }

    public RunStatus status() {
        return status;
    }

    public int progress() {
        return progress;
    }

    public int passedSteps() {
        return passedSteps;
    }

    public int failedSteps() {
        return failedSteps;
    }

    public String currentStep() {
        return currentStep;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public LocalDateTime startedAt() {
        return startedAt;
    }

    public LocalDateTime finishedAt() {
        return finishedAt;
    }

    public Path artifactDirectory() {
        return artifactDirectory;
    }

    public TestRun withProgress(RunStatus nextStatus, int nextProgress, int passed,
                                int failed, String step, String error, LocalDateTime finished) {
        return new TestRun(id, projectId, featureId, projectName, featureName, sourceFile,
                nextStatus, nextProgress, passed, failed, step, error, startedAt,
                finished, artifactDirectory);
    }
}

package com.testpilot.service;

import com.testpilot.model.entity.TestRun;
import com.testpilot.model.response.RunSummary;
import com.testpilot.model.response.StepResult;

import java.nio.file.Path;

public interface RunEventListener {
    default void onStarted(TestRun run) {
    }

    default void onProgress(TestRun run, StepResult lastStep) {
    }

    default void onPreview(TestRun run, Path screenshot) {
    }

    default void onCompleted(RunSummary summary) {
    }

    default void onUnexpectedError(TestRun run, Throwable error) {
    }
}

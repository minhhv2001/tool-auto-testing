package com.testpilot.model.response;

import com.testpilot.model.entity.TestStep;

import java.nio.file.Path;
import java.time.Duration;

public final class StepResult {
    private final TestStep step;
    private final boolean passed;
    private final String actual;
    private final String error;
    private final Duration duration;
    private final Path screenshot;

    public StepResult(TestStep step, boolean passed, String actual, String error,
                      Duration duration, Path screenshot) {
        this.step = step;
        this.passed = passed;
        this.actual = actual;
        this.error = error;
        this.duration = duration;
        this.screenshot = screenshot;
    }

    public TestStep step() {
        return step;
    }

    public boolean passed() {
        return passed;
    }

    public String actual() {
        return actual;
    }

    public String error() {
        return error;
    }

    public Duration duration() {
        return duration;
    }

    public Path screenshot() {
        return screenshot;
    }
}

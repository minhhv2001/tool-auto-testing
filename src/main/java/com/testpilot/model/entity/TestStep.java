package com.testpilot.model.entity;

import com.testpilot.model.enums.ActionType;

public final class TestStep {
    private final String testCaseId;
    private final int stepNumber;
    private final String description;
    private final ActionType action;
    private final String target;
    private final String input;
    private final String expected;
    private final int timeoutMs;
    private final boolean enabled;

    public TestStep(String testCaseId, int stepNumber, String description, ActionType action,
                    String target, String input, String expected, int timeoutMs, boolean enabled) {
        this.testCaseId = testCaseId;
        this.stepNumber = stepNumber;
        this.description = description;
        this.action = action;
        this.target = target;
        this.input = input;
        this.expected = expected;
        this.timeoutMs = timeoutMs;
        this.enabled = enabled;
    }

    public String testCaseId() {
        return testCaseId;
    }

    public int stepNumber() {
        return stepNumber;
    }

    public String description() {
        return description;
    }

    public ActionType action() {
        return action;
    }

    public String target() {
        return target;
    }

    public String input() {
        return input;
    }

    public String expected() {
        return expected;
    }

    public int timeoutMs() {
        return timeoutMs;
    }

    public boolean enabled() {
        return enabled;
    }
}

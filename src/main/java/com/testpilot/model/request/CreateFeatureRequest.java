package com.testpilot.model.request;

public final class CreateFeatureRequest {
    private final long projectId;
    private final String name;
    private final String description;

    public CreateFeatureRequest(long projectId, String name, String description) {
        this.projectId = projectId;
        this.name = name;
        this.description = description;
    }

    public long projectId() {
        return projectId;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }
}

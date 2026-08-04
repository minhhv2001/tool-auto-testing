package com.testpilot.model.entity;

import java.time.LocalDateTime;

public final class TestFeature {
    private final long id;
    private final long projectId;
    private final String name;
    private final String description;
    private final LocalDateTime createdAt;

    public TestFeature(long id, long projectId, String name, String description, LocalDateTime createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }

    public long id() {
        return id;
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

    public LocalDateTime createdAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return name;
    }
}

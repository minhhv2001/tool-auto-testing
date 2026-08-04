package com.testpilot.model.entity;

import java.time.LocalDateTime;

public final class TestProject {
    private final long id;
    private final String name;
    private final String description;
    private final String baseUrl;
    private final LocalDateTime createdAt;

    public TestProject(long id, String name, String description, String baseUrl, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.baseUrl = baseUrl;
        this.createdAt = createdAt;
    }

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return name;
    }
}

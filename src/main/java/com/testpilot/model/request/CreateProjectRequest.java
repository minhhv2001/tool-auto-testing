package com.testpilot.model.request;

public final class CreateProjectRequest {
    private final String name;
    private final String description;
    private final String baseUrl;

    public CreateProjectRequest(String name, String description, String baseUrl) {
        this.name = name;
        this.description = description;
        this.baseUrl = baseUrl;
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
}

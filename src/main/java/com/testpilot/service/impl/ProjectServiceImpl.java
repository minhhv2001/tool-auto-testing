package com.testpilot.service.impl;

import com.testpilot.model.entity.TestFeature;
import com.testpilot.model.entity.TestProject;
import com.testpilot.model.request.CreateFeatureRequest;
import com.testpilot.model.request.CreateProjectRequest;
import com.testpilot.repository.ProjectRepository;
import com.testpilot.service.ProjectService;

import java.util.List;

public final class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository repository;

    public ProjectServiceImpl(ProjectRepository repository) {
        this.repository = repository;
    }

    @Override
    public TestProject createProject(CreateProjectRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Ten project khong duoc de trong");
        }
        return repository.createProject(request);
    }

    @Override
    public TestFeature createFeature(CreateFeatureRequest request) {
        if (request.projectId() <= 0) throw new IllegalArgumentException("Hay chon project");
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Ten chuc nang khong duoc de trong");
        }
        return repository.createFeature(request);
    }

    @Override
    public List<TestProject> getProjects() {
        return repository.findAllProjects();
    }

    @Override
    public List<TestFeature> getFeatures(long projectId) {
        return repository.findFeatures(projectId);
    }
}

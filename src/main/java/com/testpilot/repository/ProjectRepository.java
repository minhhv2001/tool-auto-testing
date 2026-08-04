package com.testpilot.repository;

import com.testpilot.model.entity.TestFeature;
import com.testpilot.model.entity.TestProject;
import com.testpilot.model.request.CreateFeatureRequest;
import com.testpilot.model.request.CreateProjectRequest;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
    TestProject createProject(CreateProjectRequest request);

    TestFeature createFeature(CreateFeatureRequest request);

    List<TestProject> findAllProjects();

    List<TestFeature> findFeatures(long projectId);

    Optional<TestProject> findProject(long projectId);
}

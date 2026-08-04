package com.testpilot.service;

import com.testpilot.model.entity.TestFeature;
import com.testpilot.model.entity.TestProject;
import com.testpilot.model.request.CreateFeatureRequest;
import com.testpilot.model.request.CreateProjectRequest;

import java.util.List;

public interface ProjectService {
    TestProject createProject(CreateProjectRequest request);

    TestFeature createFeature(CreateFeatureRequest request);

    List<TestProject> getProjects();

    List<TestFeature> getFeatures(long projectId);
}

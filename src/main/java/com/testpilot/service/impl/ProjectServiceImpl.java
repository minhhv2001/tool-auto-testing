package com.testpilot.service.impl;

import com.testpilot.model.entity.TestFeature;
import com.testpilot.model.entity.TestProject;
import com.testpilot.model.request.CreateFeatureRequest;
import com.testpilot.model.request.CreateProjectRequest;
import com.testpilot.repository.ProjectRepository;
import com.testpilot.service.ProjectService;

import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.testpilot.config.AppConfig;

public final class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository repository;
    private final AppConfig config;

    public ProjectServiceImpl(ProjectRepository repository, AppConfig config) {
        this.repository = repository;
        this.config = config;
    }

    @Override
    public TestProject createProject(CreateProjectRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Tên project không được để trống");
        }
        return repository.createProject(request);
    }

    @Override
    public TestFeature createFeature(CreateFeatureRequest request) {
        if (request.projectId() <= 0) throw new IllegalArgumentException("Hãy chọn project");
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Tên chức năng không được để trống");
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

    @Override
    public void deleteProject(long projectId) {
        repository.deleteProject(projectId);
        Path projectRoot = config.dataDirectory().resolve("projects").resolve(Long.toString(projectId));
        // Xóa mọi thư mục bắt đầu bằng id- để không phụ thuộc tên project hiện tại.
        Path projectsRoot = config.dataDirectory().resolve("projects");
        if (!Files.isDirectory(projectsRoot)) return;
        try (java.util.stream.Stream<Path> paths = Files.list(projectsRoot)) {
            paths.filter(path -> path.getFileName().toString().equals(Long.toString(projectId))
                            || path.getFileName().toString().startsWith(projectId + "-"))
                    .forEach(ProjectServiceImpl::deleteRecursively);
        } catch (IOException error) {
            throw new IllegalStateException("Đã xóa project nhưng không xóa được tệp đi kèm", error);
        }
    }

    private static void deleteRecursively(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (java.util.stream.Stream<Path> children = Files.list(path)) {
                    children.forEach(ProjectServiceImpl::deleteRecursively);
                }
            }
            Files.deleteIfExists(path);
        } catch (IOException error) {
            throw new IllegalStateException("Không xóa được tệp: " + path, error);
        }
    }
}

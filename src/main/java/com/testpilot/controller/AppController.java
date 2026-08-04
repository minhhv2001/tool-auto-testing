package com.testpilot.controller;

import com.testpilot.model.entity.TestFeature;
import com.testpilot.model.entity.TestProject;
import com.testpilot.model.entity.TestRun;
import com.testpilot.model.request.CreateFeatureRequest;
import com.testpilot.model.request.CreateProjectRequest;
import com.testpilot.model.request.RunTestRequest;
import com.testpilot.model.response.ImportResult;
import com.testpilot.model.response.RunSummary;
import com.testpilot.model.response.StepResult;
import com.testpilot.service.*;
import com.testpilot.config.AppConfig;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class AppController implements AutoCloseable {
    private final ProjectService projectService;
    private final ExcelService excelService;
    private final TestRunnerService runnerService;
    private final AppConfig config;
    private final ObservableList<TestProject> projects = FXCollections.observableArrayList();
    private final ObservableList<TestRun> runs = FXCollections.observableArrayList();
    private final List<Consumer<TestRun>> runObservers = new ArrayList<>();
    private final List<BiConsumer<TestRun, Path>> previewObservers = new ArrayList<>();
    private final List<Consumer<RunSummary>> completionObservers = new ArrayList<>();

    public AppController(ProjectService projectService, ExcelService excelService, TestRunnerService runnerService, AppConfig config) {
        this.projectService = projectService;
        this.excelService = excelService;
        this.runnerService = runnerService;
        this.config = config;
        reload();
    }

    public ObservableList<TestProject> projects() {
        return projects;
    }

    public ObservableList<TestRun> runs() {
        return runs;
    }

    public List<TestFeature> features(long projectId) {
        return projectService.getFeatures(projectId);
    }

    public TestProject createProject(String name, String description, String baseUrl) {
        TestProject project = projectService.createProject(new CreateProjectRequest(name, description, baseUrl));
        projects.add(0, project);
        return project;
    }

    public TestFeature createFeature(TestProject project, String name, String description) {
        if (project == null) throw new IllegalArgumentException("Hãy chọn project trước");
        return projectService.createFeature(new CreateFeatureRequest(project.id(), name, description));
    }

    public void deleteProject(TestProject project) {
        if (project == null) throw new IllegalArgumentException("Hãy chọn project trước");
        projectService.deleteProject(project.id());
        projects.removeIf(item -> item.id() == project.id());
        runs.removeIf(run -> run.projectId() == project.id());
    }

    public AppConfig config() {
        return config;
    }

    public ImportResult validateExcel(Path file) {
        return excelService.importAutomationSteps(file);
    }

    public ImportResult validateExcel(Path file, String sheetName) {
        return excelService.importAutomationSteps(file, sheetName);
    }

    public TestRun startRun(TestProject project, TestFeature feature, Path excelFile,
                            boolean headless, String username, String password) {
        Map<String, String> secrets = new java.util.HashMap<>();
        if (username != null && !username.isBlank()) secrets.put("USERNAME", username);
        if (password != null && !password.isBlank()) secrets.put("PASSWORD", password);
        RunTestRequest request = new RunTestRequest(project, feature, excelFile, headless, secrets);
        TestRun queued = runnerService.start(request, new UiRunListener());
        replaceRun(queued);
        notifyRun(queued);
        return queued;
    }

    public boolean cancel(String runId) {
        return runnerService.cancel(runId);
    }

    public Optional<RunSummary> summary(String runId) {
        return runnerService.getSummary(runId);
    }

    public void onRunUpdate(Consumer<TestRun> observer) {
        runObservers.add(observer);
    }

    public void onPreview(BiConsumer<TestRun, Path> observer) {
        previewObservers.add(observer);
    }

    public void onCompleted(Consumer<RunSummary> observer) {
        completionObservers.add(observer);
    }

    public void reload() {
        projects.setAll(projectService.getProjects());
        runs.setAll(runnerService.getRunHistory());
    }

    @Override
    public void close() {
        runnerService.close();
    }

    private void replaceRun(TestRun run) {
        for (int i = 0; i < runs.size(); i++) {
            if (runs.get(i).id().equals(run.id())) {
                runs.set(i, run);
                return;
            }
        }
        runs.add(0, run);
    }

    private void notifyRun(TestRun run) {
        runObservers.forEach(observer -> observer.accept(run));
    }

    private final class UiRunListener implements RunEventListener {
        @Override
        public void onStarted(TestRun run) {
            update(run);
        }

        @Override
        public void onProgress(TestRun run, StepResult lastStep) {
            update(run);
        }

        @Override
        public void onPreview(TestRun run, Path screenshot) {
            Platform.runLater(() -> previewObservers.forEach(observer -> observer.accept(run, screenshot)));
        }

        @Override
        public void onCompleted(RunSummary summary) {
            Platform.runLater(() -> {
                replaceRun(summary.run());
                notifyRun(summary.run());
                completionObservers.forEach(observer -> observer.accept(summary));
            });
        }

        @Override
        public void onUnexpectedError(TestRun run, Throwable error) {
            update(run);
        }

        private void update(TestRun run) {
            Platform.runLater(() -> {
                replaceRun(run);
                notifyRun(run);
            });
        }
    }
}

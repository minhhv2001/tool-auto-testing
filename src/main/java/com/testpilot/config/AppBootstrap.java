package com.testpilot.config;

import com.testpilot.controller.AppController;
import com.testpilot.repository.ProjectRepository;
import com.testpilot.repository.RunRepository;
import com.testpilot.repository.impl.SqliteProjectRepository;
import com.testpilot.repository.impl.SqliteRunRepository;
import com.testpilot.service.ExcelService;
import com.testpilot.service.ProjectService;
import com.testpilot.service.ReportService;
import com.testpilot.service.TestRunnerService;
import com.testpilot.service.impl.ExcelServiceImpl;
import com.testpilot.service.impl.ProjectServiceImpl;
import com.testpilot.service.impl.ReportServiceImpl;
import com.testpilot.service.impl.TestRunnerServiceImpl;

import java.nio.file.Path;

public final class AppBootstrap {
    private AppBootstrap() {
    }

    public static AppController create(Path workingDirectory) {
        AppConfig config = AppConfig.load(workingDirectory);
        DatabaseManager database = new DatabaseManager(config);
        database.initialize();
        ProjectRepository projectRepository = new SqliteProjectRepository(database);
        RunRepository runRepository = new SqliteRunRepository(database);
        ProjectService projectService = new ProjectServiceImpl(projectRepository);
        ExcelService excelService = new ExcelServiceImpl();
        ReportService reportService = new ReportServiceImpl();
        TestRunnerService runnerService = new TestRunnerServiceImpl(config, excelService, reportService, runRepository);
        return new AppController(projectService, excelService, runnerService);
    }
}

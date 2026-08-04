package com.testpilot.model.response;

import com.testpilot.model.entity.TestRun;

import java.nio.file.Path;
import java.util.List;

public final class RunSummary {
    private final TestRun run;
    private final List<StepResult> results;
    private final Path reportFile;
    private final Path videoFile;
    private final Path traceFile;
    private final Path logFile;

    public RunSummary(TestRun run, List<StepResult> results, Path reportFile,
                      Path videoFile, Path traceFile, Path logFile) {
        this.run = run;
        this.results = results;
        this.reportFile = reportFile;
        this.videoFile = videoFile;
        this.traceFile = traceFile;
        this.logFile = logFile;
    }

    public TestRun run() {
        return run;
    }

    public List<StepResult> results() {
        return results;
    }

    public Path reportFile() {
        return reportFile;
    }

    public Path videoFile() {
        return videoFile;
    }

    public Path traceFile() {
        return traceFile;
    }

    public Path logFile() {
        return logFile;
    }
}

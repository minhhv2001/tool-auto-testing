package com.testpilot.service;

import com.testpilot.model.entity.TestRun;
import com.testpilot.model.request.RunTestRequest;
import com.testpilot.model.response.RunSummary;

import java.util.List;
import java.util.Optional;

public interface TestRunnerService extends AutoCloseable {
    TestRun start(RunTestRequest request, RunEventListener listener);

    boolean cancel(String runId);

    List<TestRun> getRunHistory();

    Optional<RunSummary> getSummary(String runId);

    @Override
    void close();
}

package com.testpilot.repository;

import com.testpilot.model.entity.TestRun;

import java.util.List;

public interface RunRepository {
    void save(TestRun run);

    void update(TestRun run);

    List<TestRun> findAll();
}

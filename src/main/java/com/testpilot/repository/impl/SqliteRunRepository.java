package com.testpilot.repository.impl;

import com.testpilot.config.DatabaseManager;
import com.testpilot.model.entity.TestRun;
import com.testpilot.model.enums.RunStatus;
import com.testpilot.repository.RunRepository;

import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class SqliteRunRepository implements RunRepository {
    private final DatabaseManager database;

    public SqliteRunRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public void save(TestRun run) {
        String sql = "INSERT INTO test_runs(id, project_id, feature_id, project_name, feature_name, " +
                "source_file, status, progress, passed_steps, failed_steps, current_step, " +
                "error_message, started_at, finished_at, artifact_directory) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, run);
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException("Khong luu duoc tien trinh", error);
        }
    }

    @Override
    public void update(TestRun run) {
        String sql = "UPDATE test_runs SET status=?, progress=?, passed_steps=?, failed_steps=?, " +
                "current_step=?, error_message=?, finished_at=? WHERE id=?";
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, run.status().name());
            statement.setInt(2, run.progress());
            statement.setInt(3, run.passedSteps());
            statement.setInt(4, run.failedSteps());
            statement.setString(5, safe(run.currentStep()));
            statement.setString(6, safe(run.errorMessage()));
            statement.setString(7, run.finishedAt() == null ? null : run.finishedAt().toString());
            statement.setString(8, run.id());
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException("Khong cap nhat duoc tien trinh", error);
        }
    }

    @Override
    public List<TestRun> findAll() {
        List<TestRun> runs = new ArrayList<>();
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM test_runs ORDER BY started_at DESC");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) runs.add(map(rows));
            return runs;
        } catch (SQLException error) {
            throw new IllegalStateException("Khong doc duoc lich su tien trinh", error);
        }
    }

    private static void bind(PreparedStatement statement, TestRun run) throws SQLException {
        statement.setString(1, run.id());
        statement.setLong(2, run.projectId());
        statement.setLong(3, run.featureId());
        statement.setString(4, run.projectName());
        statement.setString(5, run.featureName());
        statement.setString(6, run.sourceFile());
        statement.setString(7, run.status().name());
        statement.setInt(8, run.progress());
        statement.setInt(9, run.passedSteps());
        statement.setInt(10, run.failedSteps());
        statement.setString(11, safe(run.currentStep()));
        statement.setString(12, safe(run.errorMessage()));
        statement.setString(13, run.startedAt().toString());
        statement.setString(14, run.finishedAt() == null ? null : run.finishedAt().toString());
        statement.setString(15, run.artifactDirectory().toString());
    }

    private static TestRun map(ResultSet row) throws SQLException {
        String finished = row.getString("finished_at");
        return new TestRun(row.getString("id"), row.getLong("project_id"), row.getLong("feature_id"),
                row.getString("project_name"), row.getString("feature_name"), row.getString("source_file"),
                RunStatus.valueOf(row.getString("status")), row.getInt("progress"),
                row.getInt("passed_steps"), row.getInt("failed_steps"), row.getString("current_step"),
                row.getString("error_message"), LocalDateTime.parse(row.getString("started_at")),
                finished == null ? null : LocalDateTime.parse(finished), Path.of(row.getString("artifact_directory")));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

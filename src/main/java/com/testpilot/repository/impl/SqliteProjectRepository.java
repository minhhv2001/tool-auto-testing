package com.testpilot.repository.impl;

import com.testpilot.config.DatabaseManager;
import com.testpilot.model.entity.TestFeature;
import com.testpilot.model.entity.TestProject;
import com.testpilot.model.request.CreateFeatureRequest;
import com.testpilot.model.request.CreateProjectRequest;
import com.testpilot.repository.ProjectRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SqliteProjectRepository implements ProjectRepository {
    private final DatabaseManager database;

    public SqliteProjectRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public TestProject createProject(CreateProjectRequest request) {
        String sql = "INSERT INTO projects(name, description, base_url, created_at) VALUES(?,?,?,?)";
        LocalDateTime createdAt = LocalDateTime.now();
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, request.name().trim());
            statement.setString(2, safe(request.description()));
            statement.setString(3, safe(request.baseUrl()));
            statement.setString(4, createdAt.toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Không nhận được mã project");
                return new TestProject(keys.getLong(1), request.name().trim(), safe(request.description()),
                        safe(request.baseUrl()), createdAt);
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Không tạo được project: " + error.getMessage(), error);
        }
    }

    @Override
    public TestFeature createFeature(CreateFeatureRequest request) {
        String sql = "INSERT INTO features(project_id, name, description, created_at) VALUES(?,?,?,?)";
        LocalDateTime createdAt = LocalDateTime.now();
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, request.projectId());
            statement.setString(2, request.name().trim());
            statement.setString(3, safe(request.description()));
            statement.setString(4, createdAt.toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Không nhận được mã chức năng");
                return new TestFeature(keys.getLong(1), request.projectId(), request.name().trim(),
                        safe(request.description()), createdAt);
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Không tạo được chức năng: " + error.getMessage(), error);
        }
    }

    @Override
    public List<TestProject> findAllProjects() {
        List<TestProject> projects = new ArrayList<>();
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM projects ORDER BY created_at DESC");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) projects.add(mapProject(rows));
            return projects;
        } catch (SQLException error) {
            throw new IllegalStateException("Không đọc được danh sách project", error);
        }
    }

    @Override
    public List<TestFeature> findFeatures(long projectId) {
        List<TestFeature> features = new ArrayList<>();
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM features WHERE project_id=? ORDER BY created_at DESC")) {
            statement.setLong(1, projectId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    features.add(new TestFeature(rows.getLong("id"), rows.getLong("project_id"),
                            rows.getString("name"), rows.getString("description"),
                            LocalDateTime.parse(rows.getString("created_at"))));
                }
            }
            return features;
        } catch (SQLException error) {
            throw new IllegalStateException("Không đọc được danh sách chức năng", error);
        }
    }

    @Override
    public Optional<TestProject> findProject(long projectId) {
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM projects WHERE id=?")) {
            statement.setLong(1, projectId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(mapProject(rows)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Không đọc được project", error);
        }
    }

    @Override
    public void deleteProject(long projectId) {
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement runs = connection.prepareStatement("DELETE FROM test_runs WHERE project_id=?");
                 PreparedStatement features = connection.prepareStatement("DELETE FROM features WHERE project_id=?");
                 PreparedStatement project = connection.prepareStatement("DELETE FROM projects WHERE id=?")) {
                runs.setLong(1, projectId); runs.executeUpdate();
                features.setLong(1, projectId); features.executeUpdate();
                project.setLong(1, projectId); project.executeUpdate();
                connection.commit();
            } catch (SQLException error) {
                connection.rollback();
                throw error;
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Không xóa được project: " + error.getMessage(), error);
        }
    }

    private static TestProject mapProject(ResultSet row) throws SQLException {
        return new TestProject(row.getLong("id"), row.getString("name"), row.getString("description"),
                row.getString("base_url"), LocalDateTime.parse(row.getString("created_at")));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

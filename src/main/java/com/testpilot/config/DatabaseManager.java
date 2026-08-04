package com.testpilot.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private final String connectionUrl;

    public DatabaseManager(AppConfig config) {
        this.connectionUrl = "jdbc:sqlite:" + config.databaseFile();
    }

    public Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(connectionUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
        }
        return connection;
    }

    public void initialize() {
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS projects (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "name TEXT NOT NULL UNIQUE, " +
                            "description TEXT NOT NULL DEFAULT '', " +
                            "base_url TEXT NOT NULL DEFAULT '', " +
                            "created_at TEXT NOT NULL" +
                            ")");
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS features (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "project_id INTEGER NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "description TEXT NOT NULL DEFAULT '', " +
                            "created_at TEXT NOT NULL, " +
                            "UNIQUE(project_id, name), " +
                            "FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE" +
                            ")");
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS test_runs (" +
                            "id TEXT PRIMARY KEY, " +
                            "project_id INTEGER NOT NULL, " +
                            "feature_id INTEGER NOT NULL, " +
                            "project_name TEXT NOT NULL, " +
                            "feature_name TEXT NOT NULL, " +
                            "source_file TEXT NOT NULL, " +
                            "status TEXT NOT NULL, " +
                            "progress INTEGER NOT NULL, " +
                            "passed_steps INTEGER NOT NULL, " +
                            "failed_steps INTEGER NOT NULL, " +
                            "current_step TEXT NOT NULL DEFAULT '', " +
                            "error_message TEXT NOT NULL DEFAULT '', " +
                            "started_at TEXT NOT NULL, " +
                            "finished_at TEXT, " +
                            "artifact_directory TEXT NOT NULL" +
                            ")");
        } catch (SQLException error) {
            throw new IllegalStateException("Không khởi tạo được SQLite", error);
        }
    }
}

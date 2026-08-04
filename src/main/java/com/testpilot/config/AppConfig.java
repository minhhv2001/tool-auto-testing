package com.testpilot.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import com.testpilot.model.entity.TestProject;

public final class AppConfig {
    private final Properties properties = new Properties();
    private final Path workingDirectory;

    private AppConfig(Path workingDirectory) {
        this.workingDirectory = workingDirectory.toAbsolutePath().normalize();
    }

    public static AppConfig load(Path workingDirectory) {
        AppConfig config = new AppConfig(workingDirectory);
        Path file = config.workingDirectory.resolve("config/application.properties");
        if (Files.exists(file)) {
            try (InputStream input = Files.newInputStream(file)) {
                config.properties.load(input);
            } catch (IOException error) {
                throw new IllegalStateException("Không đọc được cấu hình: " + file, error);
            }
        }
        config.ensureDirectories();
        return config;
    }

    public String get(String key, String defaultValue) {
        return System.getProperty(key, properties.getProperty(key, defaultValue));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, Boolean.toString(defaultValue)));
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, Integer.toString(defaultValue)));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public Path dataDirectory() {
        return resolve(get("app.dataDir", "data"));
    }

    public Path outputDirectory() {
        return resolve(get("app.outputDir", "outputs"));
    }

    public Path defaultTestCaseTemplate() {
        return resolve(get("template.defaultFile", "sample-data/AUTO_TESTING_IMD_BanHang_CayChucNang.xlsx"));
    }

    public Path databaseFile() {
        return dataDirectory().resolve("testpilot.db");
    }

    public Path projectDirectory(long projectId, String projectName) {
        String safeName = projectName == null ? "project" : projectName.trim()
                .replaceAll("[^\\p{L}\\p{N}._-]+", "-")
                .replaceAll("-+", "-");
        if (safeName.isBlank()) safeName = "project";
        return dataDirectory().resolve("projects").resolve(projectId + "-" + safeName);
    }

    public Path projectDirectory(TestProject project) {
        return projectDirectory(project.id(), project.name());
    }

    public String getConfigured(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public void set(String key, String value) {
        if (value == null) properties.remove(key);
        else properties.setProperty(key, value);
    }

    public void save() {
        Path file = workingDirectory.resolve("config/application.properties");
        try {
            Files.createDirectories(file.getParent());
            try (java.io.OutputStream output = Files.newOutputStream(file)) {
                properties.store(output, "Cấu hình AUTO TESTING IMD");
            }
        } catch (IOException error) {
            throw new IllegalStateException("Không lưu được tệp cấu hình: " + file, error);
        }
    }

    public Map<String, String> testVariables() {
        Map<String, String> result = new HashMap<>();
        properties.stringPropertyNames().stream()
                .filter(key -> key.startsWith("env."))
                .forEach(key -> result.put(key.substring(4), properties.getProperty(key)));
        System.getenv().forEach((key, value) -> {
            if (key.startsWith("TESTPILOT_")) {
                result.put(key.substring("TESTPILOT_".length()), value);
            }
        });
        return result;
    }

    private Path resolve(String configuredPath) {
        Path path = Path.of(configuredPath);
        return path.isAbsolute() ? path : workingDirectory.resolve(path).normalize();
    }

    private void ensureDirectories() {
        try {
            Files.createDirectories(dataDirectory());
            Files.createDirectories(outputDirectory());
        } catch (IOException error) {
            throw new IllegalStateException("Không tạo được thư mục dữ liệu", error);
        }
    }
}

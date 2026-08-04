package com.testpilot.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
                throw new IllegalStateException("Khong doc duoc cau hinh: " + file, error);
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

    public Path databaseFile() {
        return dataDirectory().resolve("testpilot.db");
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
            throw new IllegalStateException("Khong tao duoc thu muc du lieu", error);
        }
    }
}

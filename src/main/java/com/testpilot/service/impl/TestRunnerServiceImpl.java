package com.testpilot.service.impl;

import com.microsoft.playwright.*;
import com.testpilot.config.AppConfig;
import com.testpilot.model.entity.TestRun;
import com.testpilot.model.entity.TestStep;
import com.testpilot.model.enums.RunStatus;
import com.testpilot.model.request.RunTestRequest;
import com.testpilot.model.response.ImportResult;
import com.testpilot.model.response.RunSummary;
import com.testpilot.model.response.StepResult;
import com.testpilot.repository.RunRepository;
import com.testpilot.service.ExcelService;
import com.testpilot.service.ReportService;
import com.testpilot.service.RunEventListener;
import com.testpilot.service.TestRunnerService;
import com.testpilot.util.LocatorResolver;
import com.testpilot.util.VariableResolver;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class TestRunnerServiceImpl implements TestRunnerService {
    private static final DateTimeFormatter ID_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final AppConfig config;
    private final ExcelService excelService;
    private final ReportService reportService;
    private final RunRepository runRepository;
    private final ExecutorService executor;
    private final ConcurrentMap<String, AtomicBoolean> cancellation = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RunSummary> summaries = new ConcurrentHashMap<>();

    public TestRunnerServiceImpl(AppConfig config, ExcelService excelService,
                                 ReportService reportService, RunRepository runRepository) {
        this.config = config;
        this.excelService = excelService;
        this.reportService = reportService;
        this.runRepository = runRepository;
        this.executor = Executors.newFixedThreadPool(config.getInt("runner.maxParallel", 2), runnable -> {
            Thread thread = new Thread(runnable, "testpilot-runner");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public TestRun start(RunTestRequest request, RunEventListener listener) {
        validate(request);
        ImportResult imported = excelService.importAutomationSteps(request.excelFile());
        List<TestStep> steps = imported.steps().stream()
                .filter(TestStep::enabled)
                .collect(Collectors.toList());
        String id = ID_TIME.format(LocalDateTime.now()) + "-" + UUID.randomUUID().toString().substring(0, 6);
        Path artifactDirectory = config.outputDirectory().resolve(id);
        try {
            Files.createDirectories(artifactDirectory.resolve("screenshots"));
            Files.createDirectories(artifactDirectory.resolve("video"));
        } catch (IOException error) {
            throw new IllegalStateException("Khong tao duoc thu muc ket qua", error);
        }
        TestRun run = new TestRun(id, request.project().id(), request.feature().id(),
                request.project().name(), request.feature().name(), request.excelFile().toString(),
                RunStatus.QUEUED, 0, 0, 0, "Dang cho", "", LocalDateTime.now(), null, artifactDirectory);
        runRepository.save(run);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        cancellation.put(id, cancelled);
        executor.submit(() -> execute(request, steps, run, listener == null ? new RunEventListener() {} : listener, cancelled));
        return run;
    }

    @Override
    public boolean cancel(String runId) {
        AtomicBoolean flag = cancellation.get(runId);
        if (flag == null) return false;
        flag.set(true);
        return true;
    }

    @Override
    public List<TestRun> getRunHistory() {
        return runRepository.findAll();
    }

    @Override
    public Optional<RunSummary> getSummary(String runId) {
        return Optional.ofNullable(summaries.get(runId));
    }

    @Override
    public void close() {
        cancellation.values().forEach(flag -> flag.set(true));
        executor.shutdownNow();
    }

    private void execute(RunTestRequest request, List<TestStep> steps, TestRun queued,
                         RunEventListener listener, AtomicBoolean cancelled) {
        TestRun current = queued.withProgress(RunStatus.RUNNING, 0, 0, 0, "Khoi dong trinh duyet", "", null);
        runRepository.update(current);
        TestRun startedRun = current;
        safeListener(() -> listener.onStarted(startedRun));
        List<StepResult> results = new ArrayList<>();
        Path report = current.artifactDirectory().resolve("test-results.xlsx");
        Path log = current.artifactDirectory().resolve("execution.log");
        Path trace = current.artifactDirectory().resolve("trace.zip");
        Path finalVideo = current.artifactDirectory().resolve("run-video.webm");
        String unexpectedError = "";
        Path recordedVideo = null;

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(request.headless())
                    .setSlowMo(config.getInt("runner.slowMotionMs", 120)));
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setRecordVideoDir(current.artifactDirectory().resolve("video"))
                    .setViewportSize(1440, 900)
                    .setRecordVideoSize(1440, 900);
            BrowserContext context = browser.newContext(contextOptions);
            context.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true).setSnapshots(true).setSources(true));
            Page page = context.newPage();
            page.setDefaultTimeout(config.getInt("runner.defaultTimeoutMs", 15_000));
            Video video = page.video();

            Map<String, String> variables = variables(request);
            int passed = 0;
            int failed = 0;
            for (int index = 0; index < steps.size(); index++) {
                if (cancelled.get()) break;
                TestStep step = steps.get(index);
                StepResult result = perform(page, step, variables, current.artifactDirectory(), index + 1);
                results.add(result);
                if (result.passed()) passed++; else failed++;
                int progress = (int) Math.round(((index + 1) * 100.0) / steps.size());
                RunStatus status = cancelled.get() ? RunStatus.CANCELLED : RunStatus.RUNNING;
                current = current.withProgress(status, progress, passed, failed,
                        step.testCaseId() + " · buoc " + step.stepNumber() + " · " + step.action(),
                        result.passed() ? "" : result.error(), null);
                runRepository.update(current);
                TestRun progressRun = current;
                safeListener(() -> listener.onProgress(progressRun, result));
                if (result.screenshot() != null) {
                    safeListener(() -> listener.onPreview(progressRun, result.screenshot()));
                }
            }
            context.tracing().stop(new Tracing.StopOptions().setPath(trace));
            context.close();
            if (video != null) recordedVideo = video.path();
            browser.close();
        } catch (Throwable error) {
            unexpectedError = rootMessage(error);
            TestRun failedRun = current.withProgress(RunStatus.FAILED, current.progress(), current.passedSteps(),
                    current.failedSteps() + 1, current.currentStep(), unexpectedError, LocalDateTime.now());
            safeListener(() -> listener.onUnexpectedError(failedRun, error));
        }

        if (recordedVideo != null && Files.exists(recordedVideo)) {
            try {
                Files.move(recordedVideo, finalVideo, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                finalVideo = recordedVideo;
            }
        } else {
            finalVideo = null;
        }

        int passed = (int) results.stream().filter(StepResult::passed).count();
        int failed = results.size() - passed;
        RunStatus finalStatus = cancelled.get() ? RunStatus.CANCELLED
                : (!unexpectedError.isBlank() || failed > 0 ? RunStatus.FAILED : RunStatus.PASSED);
        int finalProgress = cancelled.get() ? current.progress() : 100;
        String finalError = !unexpectedError.isBlank() ? unexpectedError
                : results.stream().filter(result -> !result.passed()).map(StepResult::error).findFirst().orElse("");
        TestRun finished = current.withProgress(finalStatus, finalProgress, passed, failed,
                finalStatus == RunStatus.CANCELLED ? "Da dung" : "Hoan tat", finalError, LocalDateTime.now());
        runRepository.update(finished);
        reportService.writeExcelReport(finished, results, report);
        reportService.writeLog(finished, results, log);
        RunSummary summary = new RunSummary(finished, List.copyOf(results), report, finalVideo,
                Files.exists(trace) ? trace : null, log);
        summaries.put(finished.id(), summary);
        cancellation.remove(finished.id());
        safeListener(() -> listener.onCompleted(summary));
    }

    private StepResult perform(Page page, TestStep rawStep, Map<String, String> variables,
                               Path artifactDirectory, int sequence) {
        TestStep step = resolve(rawStep, variables);
        Instant started = Instant.now();
        Path screenshot = artifactDirectory.resolve("screenshots")
                .resolve(String.format("%04d-%s-%02d.png", sequence, clean(step.testCaseId()), step.stepNumber()));
        String actual = "";
        try {
            page.setDefaultTimeout(step.timeoutMs());
            Locator locator;
            switch (step.action()) {
                case GOTO:
                    page.navigate(firstNotBlank(step.input(), step.target()));
                    break;
                case CLICK:
                    LocatorResolver.resolve(page, step.target()).click();
                    break;
                case FILL:
                    LocatorResolver.resolve(page, step.target()).fill(step.input());
                    break;
                case PRESS:
                    LocatorResolver.resolve(page, step.target()).press(step.input());
                    break;
                case SELECT:
                    LocatorResolver.resolve(page, step.target()).selectOption(step.input());
                    break;
                case CHECK:
                    LocatorResolver.resolve(page, step.target()).check();
                    break;
                case UNCHECK:
                    LocatorResolver.resolve(page, step.target()).uncheck();
                    break;
                case UPLOAD:
                    LocatorResolver.resolve(page, step.target()).setInputFiles(Path.of(step.input()));
                    break;
                case WAIT:
                    waitAction(page, step);
                    break;
                case EXPECT_TEXT:
                    actual = Objects.toString(LocatorResolver.resolve(page, step.target()).textContent(), "");
                    assertContains(actual, firstNotBlank(step.expected(), step.input()), "Noi dung");
                    break;
                case EXPECT_VISIBLE:
                    assertTrue(LocatorResolver.resolve(page, step.target()).isVisible(),
                            "Phan tu khong hien thi: " + step.target());
                    break;
                case EXPECT_HIDDEN:
                    assertTrue(!LocatorResolver.resolve(page, step.target()).isVisible(),
                            "Phan tu van dang hien thi: " + step.target());
                    break;
                case EXPECT_URL:
                    actual = page.url();
                    assertContains(actual, firstNotBlank(step.expected(), step.input()), "URL");
                    break;
                case EXPECT_ROWS_CONTAIN:
                    locator = LocatorResolver.resolve(page, step.target());
                    List<String> rows = locator.allInnerTexts();
                    actual = String.join(" | ", rows);
                    String expected = firstNotBlank(step.expected(), step.input());
                    assertTrue(!rows.isEmpty(), "Bang ket qua khong co dong nao");
                    List<String> invalid = rows.stream()
                            .filter(text -> !containsIgnoreCase(text, expected))
                            .collect(Collectors.toList());
                    assertTrue(invalid.isEmpty(), "Co " + invalid.size() + " dong khong chua '" + expected + "'");
                    break;
                case SCREENSHOT:
                    // Anh duoc chup o cuoi buoc.
                    break;
            }
            page.screenshot(new Page.ScreenshotOptions().setPath(screenshot).setFullPage(false));
            return new StepResult(rawStep, true, actual, "", Duration.between(started, Instant.now()), screenshot);
        } catch (Throwable error) {
            try {
                page.screenshot(new Page.ScreenshotOptions().setPath(screenshot).setFullPage(false));
            } catch (Throwable ignored) {
                screenshot = null;
            }
            return new StepResult(rawStep, false, actual, redact(rootMessage(error), variables),
                    Duration.between(started, Instant.now()), screenshot);
        }
    }

    private static void waitAction(Page page, TestStep step) {
        if (step.target() != null && !step.target().isBlank()) {
            LocatorResolver.resolve(page, step.target()).waitFor(new Locator.WaitForOptions().setTimeout(step.timeoutMs()));
            return;
        }
        double milliseconds;
        try {
            milliseconds = Double.parseDouble(firstNotBlank(step.input(), "1000"));
        } catch (NumberFormatException error) {
            milliseconds = 1000;
        }
        page.waitForTimeout(milliseconds);
    }

    private Map<String, String> variables(RunTestRequest request) {
        Map<String, String> variables = new HashMap<>(config.testVariables());
        variables.put("BASE_URL", request.project().baseUrl());
        if (request.sessionSecrets() != null) variables.putAll(request.sessionSecrets());
        return variables;
    }

    private static TestStep resolve(TestStep step, Map<String, String> variables) {
        return new TestStep(step.testCaseId(), step.stepNumber(), step.description(), step.action(),
                VariableResolver.resolve(step.target(), variables), VariableResolver.resolve(step.input(), variables),
                VariableResolver.resolve(step.expected(), variables), step.timeoutMs(), step.enabled());
    }

    private static void validate(RunTestRequest request) {
        Objects.requireNonNull(request, "Thieu yeu cau chay test");
        Objects.requireNonNull(request.project(), "Hay chon project");
        Objects.requireNonNull(request.feature(), "Hay chon chuc nang");
        Objects.requireNonNull(request.excelFile(), "Hay chon file Excel");
    }

    private static String firstNotBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private static void assertContains(String actual, String expected, String field) {
        assertTrue(containsIgnoreCase(actual, expected),
                field + " thuc te khong chua '" + expected + "'. Thuc te: " + actual);
    }

    private static boolean containsIgnoreCase(String value, String expected) {
        return value != null && expected != null
                && value.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static String clean(String text) {
        return text == null ? "case" : text.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private static String redact(String message, Map<String, String> variables) {
        String redacted = message == null ? "" : message;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey().toUpperCase(Locale.ROOT);
            String value = entry.getValue();
            if ((key.contains("PASSWORD") || key.contains("SECRET") || key.contains("TOKEN"))
                    && value != null && !value.isBlank()) {
                redacted = redacted.replace(value, "***");
            }
        }
        return redacted;
    }

    private static void safeListener(Runnable callback) {
        try {
            callback.run();
        } catch (RuntimeException ignored) {
            // Loi UI khong duoc lam dung test runner.
        }
    }
}

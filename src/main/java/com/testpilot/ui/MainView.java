package com.testpilot.ui;

import com.testpilot.controller.AppController;
import com.testpilot.model.entity.TestFeature;
import com.testpilot.model.entity.TestProject;
import com.testpilot.model.entity.TestRun;
import com.testpilot.model.enums.RunStatus;
import com.testpilot.model.response.ImportResult;
import com.testpilot.model.response.RunSummary;
import com.testpilot.ui.components.KpiCard;
import com.testpilot.ui.components.StatusBadge;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class MainView extends BorderPane {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final AppController controller;
    private final Stage stage;
    private final StackPane pages = new StackPane();
    private final Map<String, Path> previews = new HashMap<>();
    private final Map<String, Button> navigation = new HashMap<>();

    private final KpiCard projectKpi = new KpiCard("◆", "Projects", "accent-blue");
    private final KpiCard runningKpi = new KpiCard("▶", "Dang chay", "accent-indigo");
    private final KpiCard passedKpi = new KpiCard("✓", "Da Pass", "accent-green");
    private final KpiCard failedKpi = new KpiCard("!", "Can xu ly", "accent-red");
    private TableView<TestRun> dashboardRuns;
    private TableView<TestRun> runTable;
    private TreeView<Object> projectTree;
    private Label selectedProjectLabel;
    private Label selectedFeatureLabel;
    private Label excelFileLabel;
    private Label validationLabel;
    private TextField usernameField;
    private PasswordField passwordField;
    private CheckBox headlessCheck;
    private Path selectedExcelFile;
    private TestProject selectedProject;
    private TestFeature selectedFeature;

    private ImageView livePreview;
    private Label detailTitle;
    private Label detailMeta;
    private Label detailStep;
    private Label detailError;
    private ProgressBar detailProgress;
    private Label detailPercent;
    private Button stopButton;
    private Button reportButton;
    private Button videoButton;
    private Button traceButton;
    private Button folderButton;

    public MainView(AppController controller, Stage stage) {
        this.controller = controller;
        this.stage = stage;
        getStyleClass().add("app-shell");
        setLeft(buildSidebar());
        setTop(buildTopBar());
        setCenter(pages);
        pages.getChildren().addAll(buildDashboard(), buildProjects(), buildRuns(), buildSettings());
        showPage("dashboard");
        registerEvents();
        refreshAll();
    }

    private Node buildSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(24, 18, 20, 18));
        sidebar.setPrefWidth(246);

        HBox brand = new HBox(12);
        brand.setAlignment(Pos.CENTER_LEFT);
        ImageView logo = new ImageView(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/assets/testpilot-logo.png"))));
        logo.setFitWidth(44);
        logo.setFitHeight(44);
        logo.setPreserveRatio(true);
        VBox brandText = new VBox(1);
        Label name = new Label("TestPilot");
        name.getStyleClass().add("brand-name");
        Label studio = new Label("AUTOMATION STUDIO");
        studio.getStyleClass().add("brand-subtitle");
        brandText.getChildren().addAll(name, studio);
        brand.getChildren().addAll(logo, brandText);

        Label menu = new Label("KHONG GIAN LAM VIEC");
        menu.getStyleClass().add("sidebar-section");
        VBox.setMargin(menu, new Insets(26, 8, 2, 8));
        Button dashboard = navButton("dashboard", "▦", "Tong quan");
        Button projects = navButton("projects", "◇", "Projects & chuc nang");
        Button runs = navButton("runs", "▶", "Tien trinh kiem thu");
        Button settings = navButton("settings", "⚙", "Cau hinh");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        VBox tip = new VBox(8);
        tip.getStyleClass().add("sidebar-tip");
        Label tipIcon = new Label("✦  QUICK TIP");
        tipIcon.getStyleClass().add("tip-title");
        Label tipText = new Label("Uu tien target theo data-testid de testcase ben vung khi giao dien thay doi.");
        tipText.setWrapText(true);
        tipText.getStyleClass().add("tip-text");
        tip.getChildren().addAll(tipIcon, tipText);
        sidebar.getChildren().addAll(brand, menu, dashboard, projects, runs, settings, spacer, tip);
        return sidebar;
    }

    private Node buildTopBar() {
        HBox top = new HBox(14);
        top.getStyleClass().add("topbar");
        top.setPadding(new Insets(16, 30, 16, 30));
        top.setAlignment(Pos.CENTER_LEFT);
        Label environment = new Label("●  LOCAL RUNNER");
        environment.getStyleClass().add("environment-pill");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label clockHint = new Label("JavaFX · Playwright · SQLite");
        clockHint.getStyleClass().add("muted");
        Button quickRun = primaryButton("＋  Tao tien trinh");
        quickRun.setOnAction(event -> showPage("projects"));
        top.getChildren().addAll(environment, spacer, clockHint, quickRun);
        return top;
    }

    private Node buildDashboard() {
        VBox content = page("dashboard");
        content.getChildren().add(pageHeading("Xin chao, Minh", "Theo doi chat luong website va cac tien trinh tu dong tai mot noi."));

        GridPane kpis = new GridPane();
        kpis.setHgap(16);
        ColumnConstraints one = new ColumnConstraints();
        one.setPercentWidth(25);
        kpis.getColumnConstraints().addAll(one, cloneColumn(one), cloneColumn(one), cloneColumn(one));
        kpis.add(projectKpi, 0, 0);
        kpis.add(runningKpi, 1, 0);
        kpis.add(passedKpi, 2, 0);
        kpis.add(failedKpi, 3, 0);

        VBox recent = card();
        HBox title = sectionHeader("Tien trinh gan day", "Theo doi truc tiep tien do, Pass/Fail va loi moi nhat");
        Button viewAll = secondaryButton("Xem tat ca  →");
        viewAll.setOnAction(event -> showPage("runs"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        title.getChildren().addAll(spacer, viewAll);
        dashboardRuns = createRunTable(false);
        dashboardRuns.setPrefHeight(370);
        recent.getChildren().addAll(title, dashboardRuns);

        HBox flow = new HBox(16);
        VBox importCard = actionCard("01", "Import Excel", "Tai file co sheet Automation Steps va kiem tra cau truc truoc khi chay.");
        VBox runCard = actionCard("02", "Chay nhu nguoi dung", "Chrome thao tac click, fill, upload va doi chieu ket qua mong doi.");
        VBox evidenceCard = actionCard("03", "Bang chung day du", "Luu anh, video, trace, log chi tiet va bao cao Excel sau moi lan chay.");
        HBox.setHgrow(importCard, Priority.ALWAYS);
        HBox.setHgrow(runCard, Priority.ALWAYS);
        HBox.setHgrow(evidenceCard, Priority.ALWAYS);
        flow.getChildren().addAll(importCard, runCard, evidenceCard);

        content.getChildren().addAll(kpis, recent, flow);
        return scroll(content);
    }

    private Node buildProjects() {
        VBox content = page("projects");
        HBox heading = pageHeading("Projects & chuc nang", "To chuc testcase theo tung he thong va nghiep vu can kiem thu.");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button newProject = primaryButton("＋  Project moi");
        newProject.setOnAction(event -> createProjectDialog());
        heading.getChildren().addAll(spacer, newProject);

        SplitPane split = new SplitPane();
        split.getStyleClass().add("workspace-split");
        split.setDividerPositions(0.31);

        VBox treeCard = card();
        treeCard.setMinWidth(300);
        HBox treeHeader = sectionHeader("Cau truc kiem thu", "Chon project hoac chuc nang");
        Button addFeature = iconButton("＋");
        addFeature.setTooltip(new Tooltip("Them chuc nang vao project dang chon"));
        addFeature.setOnAction(event -> createFeatureDialog());
        Region treeSpacer = new Region();
        HBox.setHgrow(treeSpacer, Priority.ALWAYS);
        treeHeader.getChildren().addAll(treeSpacer, addFeature);
        projectTree = new TreeView<>();
        projectTree.setShowRoot(false);
        projectTree.setCellFactory(tree -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item instanceof TestProject) {
                    TestProject project = (TestProject) item;
                    setText("◇  " + project.name());
                    getStyleClass().add("project-tree-item");
                } else if (item instanceof TestFeature) {
                    TestFeature feature = (TestFeature) item;
                    setText("  └  " + feature.name());
                }
            }
        });
        VBox.setVgrow(projectTree, Priority.ALWAYS);
        treeCard.getChildren().addAll(treeHeader, projectTree);

        VBox runCard = card();
        HBox runHeader = sectionHeader("Tao tien trinh kiem thu", "Chon file Excel, thong tin dang nhap va che do chay");
        selectedProjectLabel = valueLabel("Chua chon project");
        selectedFeatureLabel = valueLabel("Chua chon chuc nang");
        GridPane selection = formGrid();
        selection.add(fieldLabel("Project"), 0, 0);
        selection.add(selectedProjectLabel, 1, 0);
        selection.add(fieldLabel("Chuc nang"), 0, 1);
        selection.add(selectedFeatureLabel, 1, 1);

        VBox fileDrop = new VBox(8);
        fileDrop.getStyleClass().add("file-drop");
        fileDrop.setAlignment(Pos.CENTER);
        Label fileIcon = new Label("⇧");
        fileIcon.getStyleClass().add("file-icon");
        Label fileTitle = new Label("Chon file testcase Excel");
        fileTitle.getStyleClass().add("file-title");
        excelFileLabel = new Label("Ho tro .xlsx · can co sheet Automation Steps");
        excelFileLabel.getStyleClass().add("muted");
        Button browse = secondaryButton("Duyet file");
        browse.setOnAction(event -> chooseExcelFile());
        fileDrop.getChildren().addAll(fileIcon, fileTitle, excelFileLabel, browse);

        validationLabel = new Label("Chua kiem tra file");
        validationLabel.getStyleClass().addAll("validation-message", "validation-neutral");
        Button validate = secondaryButton("✓  Kiem tra file");
        validate.setOnAction(event -> validateExcel());

        usernameField = new TextField();
        usernameField.setPromptText("${USERNAME} - co the de trong neu Excel khong dung");
        passwordField = new PasswordField();
        passwordField.setPromptText("${PASSWORD} - chi ton tai trong phien chay");
        headlessCheck = new CheckBox("Chay an trinh duyet (headless)");
        headlessCheck.setSelected(false);
        GridPane credentials = formGrid();
        credentials.add(fieldLabel("Tai khoan test"), 0, 0);
        credentials.add(usernameField, 1, 0);
        credentials.add(fieldLabel("Mat khau test"), 0, 1);
        credentials.add(passwordField, 1, 1);
        credentials.add(new Label(""), 0, 2);
        credentials.add(headlessCheck, 1, 2);

        HBox actions = new HBox(12, validate, validationLabel);
        actions.setAlignment(Pos.CENTER_LEFT);
        Button start = primaryButton("▶  CHAY KIEM THU");
        start.getStyleClass().add("run-button");
        start.setMaxWidth(Double.MAX_VALUE);
        start.setOnAction(event -> startRun());
        runCard.getChildren().addAll(runHeader, selection, separator(), fileDrop, actions,
                separator(), credentials, start);

        split.getItems().addAll(treeCard, runCard);
        content.getChildren().addAll(heading, split);
        VBox.setVgrow(split, Priority.ALWAYS);
        return content;
    }

    private Node buildRuns() {
        VBox content = page("runs");
        content.getChildren().add(pageHeading("Tien trinh kiem thu", "Quan ly tien trinh dang chay va xem lai toan bo bang chung cua lich su test."));
        SplitPane split = new SplitPane();
        split.getStyleClass().add("workspace-split");
        split.setDividerPositions(0.56);

        VBox tableCard = card();
        HBox header = sectionHeader("Danh sach tien trinh", "Bam mot dong de xem video, anh, trace va report");
        Button refresh = secondaryButton("↻  Lam moi");
        refresh.setOnAction(event -> refreshAll());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(spacer, refresh);
        runTable = createRunTable(true);
        VBox.setVgrow(runTable, Priority.ALWAYS);
        tableCard.getChildren().addAll(header, runTable);

        VBox detail = card();
        detailTitle = new Label("Chon mot tien trinh");
        detailTitle.getStyleClass().add("section-title");
        detailMeta = new Label("Thong tin va bang chung se hien thi tai day.");
        detailMeta.getStyleClass().add("muted");
        detailProgress = new ProgressBar(0);
        detailProgress.setMaxWidth(Double.MAX_VALUE);
        detailPercent = valueLabel("0%");
        HBox progress = new HBox(12, detailProgress, detailPercent);
        progress.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(detailProgress, Priority.ALWAYS);
        detailStep = new Label("Chua co buoc nao");
        detailStep.getStyleClass().add("current-step");
        detailError = new Label("");
        detailError.setWrapText(true);
        detailError.getStyleClass().add("error-text");

        StackPane previewFrame = new StackPane();
        previewFrame.getStyleClass().add("preview-frame");
        livePreview = new ImageView();
        livePreview.setPreserveRatio(true);
        livePreview.setFitWidth(610);
        livePreview.setFitHeight(320);
        Label placeholder = new Label("LIVE PREVIEW\nAnh trinh duyet se cap nhat sau moi buoc");
        placeholder.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        placeholder.getStyleClass().add("preview-placeholder");
        livePreview.imageProperty().addListener((observable, oldImage, newImage) -> placeholder.setVisible(newImage == null));
        previewFrame.getChildren().addAll(placeholder, livePreview);

        stopButton = dangerButton("■  Dung");
        stopButton.setOnAction(event -> stopSelectedRun());
        reportButton = secondaryButton("Excel");
        reportButton.setOnAction(event -> openArtifact("test-results.xlsx"));
        videoButton = secondaryButton("Video");
        videoButton.setOnAction(event -> openArtifact("run-video.webm"));
        traceButton = secondaryButton("Trace");
        traceButton.setOnAction(event -> openArtifact("trace.zip"));
        folderButton = secondaryButton("Thu muc");
        folderButton.setOnAction(event -> openArtifact(""));
        HBox artifactButtons = new HBox(8, stopButton, reportButton, videoButton, traceButton, folderButton);
        detail.getChildren().addAll(detailTitle, detailMeta, progress, detailStep, detailError,
                previewFrame, artifactButtons);
        VBox.setVgrow(previewFrame, Priority.ALWAYS);

        split.getItems().addAll(tableCard, detail);
        content.getChildren().add(split);
        VBox.setVgrow(split, Priority.ALWAYS);
        return content;
    }

    private Node buildSettings() {
        VBox content = page("settings");
        content.getChildren().add(pageHeading("Cau hinh", "Tat ca link va tuy chon runner nam ngoai code de thay doi de dang."));
        HBox row = new HBox(18);
        VBox environment = card();
        environment.getChildren().addAll(sectionHeader("Moi truong", "File config/application.properties"),
                infoRow("BASE_URL", "env.BASE_URL hoac URL cua project"),
                infoRow("USERNAME", "Bien moi truong TESTPILOT_USERNAME"),
                infoRow("PASSWORD", "Bien moi truong TESTPILOT_PASSWORD"),
                infoRow("Headless", "runner.headless=false"),
                infoRow("Timeout", "runner.defaultTimeoutMs=15000"));
        VBox privacy = card();
        privacy.getChildren().addAll(sectionHeader("An toan du lieu", "Khong dua tai khoan vao testcase"),
                bullet("Mat khau nhap tren man hinh chi song trong phien chay."),
                bullet("Report khong ghi lai gia tri cua bien PASSWORD."),
                bullet("File secrets.properties that bi loai khoi Git."),
                bullet("Ban production nen tich hop Windows Credential Manager."));
        HBox.setHgrow(environment, Priority.ALWAYS);
        HBox.setHgrow(privacy, Priority.ALWAYS);
        row.getChildren().addAll(environment, privacy);

        VBox actions = card();
        actions.getChildren().addAll(sectionHeader("Action ho tro", "Tu khoa dung trong cot Action cua Excel"),
                wrapChips("goto", "click", "fill", "press", "select", "check", "uncheck", "upload",
                        "wait", "expectText", "expectVisible", "expectHidden", "expectUrl",
                        "expectRowsContain", "screenshot"));
        content.getChildren().addAll(row, actions);
        return scroll(content);
    }

    private void registerEvents() {
        controller.runs().addListener((ListChangeListener<TestRun>) change -> refreshMetrics());
        controller.onRunUpdate(run -> {
            refreshMetrics();
            dashboardRuns.refresh();
            runTable.refresh();
            if (isSelected(run)) showRunDetail(run);
        });
        controller.onPreview((run, screenshot) -> {
            previews.put(run.id(), screenshot);
            if (isSelected(run)) setPreview(screenshot);
        });
        controller.onCompleted(summary -> {
            refreshMetrics();
            if (isSelected(summary.run())) showRunDetail(summary.run());
        });
        projectTree.getSelectionModel().selectedItemProperty().addListener((observable, oldItem, newItem) -> {
            if (newItem == null) return;
            Object value = newItem.getValue();
            if (value instanceof TestProject) {
                TestProject project = (TestProject) value;
                selectedProject = project;
                selectedFeature = null;
            } else if (value instanceof TestFeature) {
                TestFeature feature = (TestFeature) value;
                selectedFeature = feature;
                TreeItem<Object> parent = newItem.getParent();
                selectedProject = parent != null && parent.getValue() instanceof TestProject
                        ? (TestProject) parent.getValue() : null;
            }
            refreshSelectionLabels();
        });
        runTable.getSelectionModel().selectedItemProperty().addListener((observable, oldRun, run) -> showRunDetail(run));
    }

    private void refreshAll() {
        controller.reload();
        refreshProjectTree();
        if (dashboardRuns != null) dashboardRuns.setItems(controller.runs());
        if (runTable != null) runTable.setItems(controller.runs());
        refreshMetrics();
        if (runTable != null && !controller.runs().isEmpty() && runTable.getSelectionModel().getSelectedItem() == null) {
            runTable.getSelectionModel().selectFirst();
        }
    }

    private void refreshMetrics() {
        long running = controller.runs().stream().filter(run -> run.status() == RunStatus.RUNNING || run.status() == RunStatus.QUEUED).count();
        long passed = controller.runs().stream().filter(run -> run.status() == RunStatus.PASSED).count();
        long failed = controller.runs().stream().filter(run -> run.status() == RunStatus.FAILED).count();
        projectKpi.setValue(Integer.toString(controller.projects().size()));
        projectKpi.setCaption("he thong da cau hinh");
        runningKpi.setValue(Long.toString(running));
        runningKpi.setCaption(running > 0 ? "dang thao tac tren trinh duyet" : "runner dang san sang");
        passedKpi.setValue(Long.toString(passed));
        passedKpi.setCaption("tien trinh hoan thanh tot");
        failedKpi.setValue(Long.toString(failed));
        failedKpi.setCaption("tien trinh co loi");
    }

    private void refreshProjectTree() {
        TreeItem<Object> root = new TreeItem<>("root");
        root.setExpanded(true);
        for (TestProject project : controller.projects()) {
            TreeItem<Object> projectItem = new TreeItem<>(project);
            projectItem.setExpanded(true);
            controller.features(project.id()).forEach(feature -> projectItem.getChildren().add(new TreeItem<>(feature)));
            root.getChildren().add(projectItem);
        }
        projectTree.setRoot(root);
    }

    private void createProjectDialog() {
        Dialog<ButtonType> dialog = formDialog("Tao project moi", "Project dai dien cho mot website hoac mot he thong can test.");
        TextField name = new TextField();
        name.setPromptText("VD: Cong thong tin noi bo");
        TextField url = new TextField();
        url.setPromptText("https://staging.example.com");
        TextArea description = new TextArea();
        description.setPromptText("Mo ta ngan ve he thong...");
        description.setPrefRowCount(3);
        GridPane form = formGrid();
        form.add(fieldLabel("Ten project"), 0, 0); form.add(name, 1, 0);
        form.add(fieldLabel("Base URL"), 0, 1); form.add(url, 1, 1);
        form.add(fieldLabel("Mo ta"), 0, 2); form.add(description, 1, 2);
        dialog.getDialogPane().setContent(form);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                TestProject created = controller.createProject(name.getText(), description.getText(), url.getText());
                refreshProjectTree();
                selectTreeValue(created);
                showMessage(Alert.AlertType.INFORMATION, "Da tao project", "Ban co the them chuc nang va import testcase ngay bay gio.");
            } catch (RuntimeException error) {
                showError(error);
            }
        }
    }

    private void createFeatureDialog() {
        if (selectedProject == null) {
            showMessage(Alert.AlertType.WARNING, "Chua chon project", "Hay chon mot project truoc khi tao chuc nang.");
            return;
        }
        Dialog<ButtonType> dialog = formDialog("Them chuc nang", "Project: " + selectedProject.name());
        TextField name = new TextField();
        name.setPromptText("VD: Danh sach & bo loc tim kiem");
        TextArea description = new TextArea();
        description.setPrefRowCount(3);
        GridPane form = formGrid();
        form.add(fieldLabel("Ten chuc nang"), 0, 0); form.add(name, 1, 0);
        form.add(fieldLabel("Mo ta"), 0, 1); form.add(description, 1, 1);
        dialog.getDialogPane().setContent(form);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                TestFeature created = controller.createFeature(selectedProject, name.getText(), description.getText());
                refreshProjectTree();
                selectTreeValue(created);
            } catch (RuntimeException error) {
                showError(error);
            }
        }
    }

    private void chooseExcelFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chon file testcase Excel");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel workbook", "*.xlsx"));
        java.io.File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        selectedExcelFile = file.toPath();
        excelFileLabel.setText(file.getName());
        setValidation("Chua kiem tra file moi", false, false);
    }

    private void validateExcel() {
        if (selectedExcelFile == null) {
            showMessage(Alert.AlertType.WARNING, "Chua co file", "Hay chon file Excel truoc.");
            return;
        }
        try {
            ImportResult result = controller.validateExcel(selectedExcelFile);
            setValidation("Hop le · " + result.testCaseCount() + " testcase · " + result.steps().size() + " buoc", true, false);
        } catch (RuntimeException error) {
            setValidation("Khong hop le · " + error.getMessage(), false, true);
        }
    }

    private void startRun() {
        try {
            if (selectedProject == null) throw new IllegalArgumentException("Hay chon project");
            if (selectedFeature == null) throw new IllegalArgumentException("Hay chon chuc nang");
            if (selectedExcelFile == null) throw new IllegalArgumentException("Hay chon file Excel");
            controller.validateExcel(selectedExcelFile);
            TestRun run = controller.startRun(selectedProject, selectedFeature, selectedExcelFile,
                    headlessCheck.isSelected(), usernameField.getText(), passwordField.getText());
            passwordField.clear();
            showPage("runs");
            runTable.getSelectionModel().select(run);
            showRunDetail(run);
        } catch (RuntimeException error) {
            showError(error);
        }
    }

    private void stopSelectedRun() {
        TestRun run = runTable.getSelectionModel().getSelectedItem();
        if (run == null) return;
        if (!controller.cancel(run.id())) {
            showMessage(Alert.AlertType.INFORMATION, "Khong co tien trinh dang chay", "Tien trinh nay da ket thuc.");
        }
    }

    private void showRunDetail(TestRun run) {
        if (run == null || detailTitle == null) return;
        detailTitle.setText(run.projectName() + "  /  " + run.featureName());
        detailMeta.setText(DATE_TIME.format(run.startedAt()) + "  ·  " + Path.of(run.sourceFile()).getFileName());
        detailProgress.setProgress(run.progress() / 100.0);
        detailPercent.setText(run.progress() + "%");
        detailStep.setText(run.currentStep() == null || run.currentStep().isBlank() ? "Chua co buoc nao" : run.currentStep());
        detailError.setText(run.errorMessage() == null ? "" : run.errorMessage());
        stopButton.setDisable(run.status() != RunStatus.RUNNING && run.status() != RunStatus.QUEUED);
        reportButton.setDisable(!Files.exists(run.artifactDirectory().resolve("test-results.xlsx")));
        videoButton.setDisable(!Files.exists(run.artifactDirectory().resolve("run-video.webm")));
        traceButton.setDisable(!Files.exists(run.artifactDirectory().resolve("trace.zip")));
        folderButton.setDisable(!Files.exists(run.artifactDirectory()));
        Path preview = previews.get(run.id());
        if (preview == null) preview = latestScreenshot(run.artifactDirectory().resolve("screenshots"));
        if (preview != null) setPreview(preview); else livePreview.setImage(null);
    }

    private void setPreview(Path screenshot) {
        if (screenshot == null || !Files.exists(screenshot)) return;
        livePreview.setImage(new Image(screenshot.toUri().toString(), false));
    }

    private static Path latestScreenshot(Path directory) {
        if (!Files.isDirectory(directory)) return null;
        try (var paths = Files.list(directory)) {
            return paths.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
                    .max(java.util.Comparator.comparing(path -> path.getFileName().toString()))
                    .orElse(null);
        } catch (IOException ignored) {
            return null;
        }
    }

    private void openArtifact(String filename) {
        TestRun run = runTable.getSelectionModel().getSelectedItem();
        if (run == null) return;
        Path path = filename.isBlank() ? run.artifactDirectory() : run.artifactDirectory().resolve(filename);
        if (!Files.exists(path)) {
            showMessage(Alert.AlertType.WARNING, "Chua co tep", "Tep nay chi co sau khi tien trinh hoan tat.");
            return;
        }
        try {
            Desktop.getDesktop().open(path.toFile());
        } catch (IOException | UnsupportedOperationException error) {
            showMessage(Alert.AlertType.ERROR, "Khong mo duoc tep", path.toAbsolutePath().toString());
        }
    }

    private TableView<TestRun> createRunTable(boolean full) {
        TableView<TestRun> table = new TableView<>(controller.runs());
        table.getStyleClass().add("run-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Chua co tien trinh. Hay tao project va chay testcase dau tien."));
        TableColumn<TestRun, String> time = new TableColumn<>("BAT DAU");
        time.setCellValueFactory(data -> new ReadOnlyStringWrapper(DATE_TIME.format(data.getValue().startedAt())));
        time.setPrefWidth(142);
        TableColumn<TestRun, String> name = new TableColumn<>("PROJECT / CHUC NANG");
        name.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().projectName() + "\n" + data.getValue().featureName()));
        name.setPrefWidth(220);
        TableColumn<TestRun, RunStatus> status = new TableColumn<>("TRANG THAI");
        status.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().status()));
        status.setCellFactory(column -> new TableCell<>() {
            private final StatusBadge badge = new StatusBadge();
            @Override
            protected void updateItem(RunStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else { badge.setStatus(item); setGraphic(badge); }
            }
        });
        status.setPrefWidth(105);
        TableColumn<TestRun, Number> progress = new TableColumn<>("TIEN DO");
        progress.setCellValueFactory(data -> new ReadOnlyIntegerWrapper(data.getValue().progress()));
        progress.setCellFactory(column -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar();
            private final Label percent = new Label();
            private final HBox box = new HBox(6, bar, percent);
            { box.setAlignment(Pos.CENTER_LEFT); bar.setPrefWidth(82); }
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else { bar.setProgress(item.doubleValue() / 100.0); percent.setText(item.intValue() + "%"); setGraphic(box); }
            }
        });
        progress.setPrefWidth(135);
        TableColumn<TestRun, String> score = new TableColumn<>("KET QUA");
        score.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                "✓ " + data.getValue().passedSteps() + "   ✕ " + data.getValue().failedSteps()));
        score.setPrefWidth(90);
        table.getColumns().addAll(time, name, status, progress, score);
        if (full) {
            TableColumn<TestRun, String> file = new TableColumn<>("FILE");
            file.setCellValueFactory(data -> new ReadOnlyStringWrapper(Path.of(data.getValue().sourceFile()).getFileName().toString()));
            file.setPrefWidth(150);
            table.getColumns().add(file);
        }
        return table;
    }

    private Button navButton(String id, String icon, String text) {
        Button button = new Button(icon + "   " + text);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(event -> showPage(id));
        navigation.put(id, button);
        return button;
    }

    private void showPage(String id) {
        pages.getChildren().forEach(node -> node.setVisible(id.equals(node.getUserData())));
        navigation.forEach((key, button) -> button.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("selected"), key.equals(id)));
        if (id.equals("runs") && runTable != null) runTable.refresh();
    }

    private static VBox page(String id) {
        VBox page = new VBox(20);
        page.setUserData(id);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(28, 30, 30, 30));
        return page;
    }

    private static ScrollPane scroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        scroll.setUserData(content.getUserData());
        return scroll;
    }

    private static HBox pageHeading(String title, String subtitle) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox text = new VBox(4);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("page-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("page-subtitle");
        text.getChildren().addAll(titleLabel, subtitleLabel);
        row.getChildren().add(text);
        return row;
    }

    private static HBox sectionHeader(String title, String subtitle) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox text = new VBox(3);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("muted");
        text.getChildren().addAll(titleLabel, subtitleLabel);
        row.getChildren().add(text);
        return row;
    }

    private static VBox card() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(22));
        return card;
    }

    private static VBox actionCard(String number, String title, String text) {
        VBox card = card();
        card.setMaxWidth(Double.MAX_VALUE);
        Label index = new Label(number);
        index.getStyleClass().add("flow-number");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");
        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        textLabel.getStyleClass().add("muted");
        card.getChildren().addAll(index, titleLabel, textLabel);
        return card;
    }

    private static Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        return button;
    }

    private static Button secondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");
        return button;
    }

    private static Button dangerButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("danger-button");
        return button;
    }

    private static Button iconButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("icon-button");
        return button;
    }

    private static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private static Label valueLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("value-label");
        return label;
    }

    private static GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(13);
        ColumnConstraints labels = new ColumnConstraints(132);
        ColumnConstraints inputs = new ColumnConstraints();
        inputs.setHgrow(Priority.ALWAYS);
        inputs.setFillWidth(true);
        grid.getColumnConstraints().addAll(labels, inputs);
        return grid;
    }

    private static Separator separator() {
        Separator separator = new Separator();
        separator.getStyleClass().add("soft-separator");
        return separator;
    }

    private static Dialog<ButtonType> formDialog(String title, String subtitle) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(subtitle);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(560);
        return dialog;
    }

    private static HBox infoRow(String key, String value) {
        HBox row = new HBox(12);
        Label first = valueLabel(key);
        first.setMinWidth(110);
        Label second = new Label(value);
        second.getStyleClass().add("muted");
        row.getChildren().addAll(first, second);
        return row;
    }

    private static Label bullet(String text) {
        Label label = new Label("✓  " + text);
        label.getStyleClass().add("settings-bullet");
        label.setWrapText(true);
        return label;
    }

    private static FlowPane wrapChips(String... values) {
        FlowPane pane = new FlowPane(8, 8);
        for (String value : values) {
            Label chip = new Label(value);
            chip.getStyleClass().add("action-chip");
            pane.getChildren().add(chip);
        }
        return pane;
    }

    private static ColumnConstraints cloneColumn(ColumnConstraints source) {
        ColumnConstraints copy = new ColumnConstraints();
        copy.setPercentWidth(source.getPercentWidth());
        return copy;
    }

    private void setValidation(String text, boolean success, boolean error) {
        validationLabel.setText(text);
        validationLabel.getStyleClass().removeAll("validation-neutral", "validation-success", "validation-error");
        validationLabel.getStyleClass().add(success ? "validation-success" : error ? "validation-error" : "validation-neutral");
    }

    private void refreshSelectionLabels() {
        selectedProjectLabel.setText(selectedProject == null ? "Chua chon project" : selectedProject.name());
        selectedFeatureLabel.setText(selectedFeature == null ? "Chua chon chuc nang" : selectedFeature.name());
    }

    private void selectTreeValue(Object value) {
        for (TreeItem<Object> projectItem : projectTree.getRoot().getChildren()) {
            if (sameEntity(projectItem.getValue(), value)) {
                projectTree.getSelectionModel().select(projectItem);
                return;
            }
            for (TreeItem<Object> featureItem : projectItem.getChildren()) {
                if (sameEntity(featureItem.getValue(), value)) {
                    projectTree.getSelectionModel().select(featureItem);
                    return;
                }
            }
        }
    }

    private static boolean sameEntity(Object left, Object right) {
        if (left instanceof TestProject && right instanceof TestProject) {
            return ((TestProject) left).id() == ((TestProject) right).id();
        }
        if (left instanceof TestFeature && right instanceof TestFeature) {
            return ((TestFeature) left).id() == ((TestFeature) right).id();
        }
        return Objects.equals(left, right);
    }

    private boolean isSelected(TestRun run) {
        return runTable != null && runTable.getSelectionModel().getSelectedItem() != null
                && runTable.getSelectionModel().getSelectedItem().id().equals(run.id());
    }

    private void showError(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        showMessage(Alert.AlertType.ERROR, "Khong the thuc hien", current.getMessage());
    }

    private void showMessage(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.initOwner(stage);
        alert.setTitle("TestPilot Studio");
        alert.setHeaderText(title);
        alert.setContentText(message == null ? "Loi khong xac dinh" : message);
        alert.showAndWait();
    }
}

package com.testpilot.ui;

import com.testpilot.controller.AppController;
import com.testpilot.model.entity.TestFeature;
import com.testpilot.model.entity.TestProject;
import com.testpilot.model.entity.TestRun;
import com.testpilot.model.enums.RunStatus;
import com.testpilot.model.response.ImportResult;
import com.testpilot.ui.components.KpiCard;
import com.testpilot.ui.components.StatusBadge;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
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
import java.util.*;
import java.util.stream.Collectors;

public final class MainView extends BorderPane {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final AppController controller;
    private final Stage stage;
    private final StackPane pages = new StackPane();
    private final Map<String, Button> navigation = new HashMap<>();
    private final Map<String, Path> previews = new HashMap<>();
    private final KpiCard projectKpi = new KpiCard("◆", "Dự án", "accent-blue");
    private final KpiCard runningKpi = new KpiCard("▶", "Đang chạy", "accent-indigo");
    private final KpiCard passedKpi = new KpiCard("✓", "Đạt", "accent-green");
    private final KpiCard failedKpi = new KpiCard("!", "Không đạt", "accent-red");
    private TableView<TestRun> dashboardRuns;
    private TableView<TestRun> runTable;
    private FilteredList<TestRun> filteredRuns;
    private ComboBox<String> projectFilter;
    private ComboBox<String> featureFilter;
    private TreeView<Object> projectTree;
    private ListView<String> errorList;
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
    private TextField configUrlField;
    private TextField configUsernameField;
    private PasswordField configPasswordField;
    private CheckBox configHeadlessCheck;
    private TextField configTimeoutField;
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
        registerEvents();
        showPage("dashboard");
        refreshAll();
    }

    private Node buildSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(24, 18, 20, 18));
        sidebar.setPrefWidth(246);
        HBox brand = new HBox(12);
        brand.setAlignment(Pos.CENTER_LEFT);
        ImageView logo = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/testpilot-logo.png"))));
        logo.setFitWidth(44); logo.setFitHeight(44); logo.setPreserveRatio(true);
        VBox brandText = new VBox(1);
        Label name = new Label("TestPilot"); name.getStyleClass().add("brand-name");
        Label studio = new Label("AUTOMATION STUDIO"); studio.getStyleClass().add("brand-subtitle");
        brandText.getChildren().addAll(name, studio); brand.getChildren().addAll(logo, brandText);
        Label menu = new Label("KHÔNG GIAN LÀM VIỆC"); menu.getStyleClass().add("sidebar-section");
        VBox.setMargin(menu, new Insets(26, 8, 2, 8));
        Button dashboard = navButton("dashboard", "◆", "Tổng quan");
        Button projects = navButton("projects", "◇", "Dự án & chức năng");
        Button runs = navButton("runs", "▶", "Tiến trình kiểm thử");
        Button settings = navButton("settings", "⚙", "Cấu hình");
        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        VBox tip = new VBox(8); tip.getStyleClass().add("sidebar-tip");
        Label tipTitle = new Label("✦  GỢI Ý"); tipTitle.getStyleClass().add("tip-title");
        Label tipText = new Label("Ưu tiên target theo data-testid để testcase bền vững khi giao diện thay đổi.");
        tipText.setWrapText(true); tipText.getStyleClass().add("tip-text"); tip.getChildren().addAll(tipTitle, tipText);
        sidebar.getChildren().addAll(brand, menu, dashboard, projects, runs, settings, spacer, tip);
        return sidebar;
    }

    private Node buildTopBar() {
        HBox top = new HBox(14); top.getStyleClass().add("topbar"); top.setPadding(new Insets(16, 30, 16, 30)); top.setAlignment(Pos.CENTER_LEFT);
        Label environment = new Label("●  CHẠY CỤC BỘ"); environment.getStyleClass().add("environment-pill");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label stack = new Label("JavaFX · Playwright · SQLite"); stack.getStyleClass().add("muted");
        Button quickRun = primaryButton("＋  Tạo tiến trình"); quickRun.setOnAction(event -> showPage("projects"));
        top.getChildren().addAll(environment, spacer, stack, quickRun); return top;
    }

    private Node buildDashboard() {
        VBox content = page("dashboard");
        content.getChildren().add(pageHeading("Xin chào, Minh", "Theo dõi chất lượng website và các tiến trình tự động tại một nơi."));
        GridPane kpis = new GridPane(); kpis.setHgap(16);
        ColumnConstraints one = new ColumnConstraints(); one.setPercentWidth(25);
        kpis.getColumnConstraints().addAll(one, cloneColumn(one), cloneColumn(one), cloneColumn(one));
        kpis.add(projectKpi, 0, 0); kpis.add(runningKpi, 1, 0); kpis.add(passedKpi, 2, 0); kpis.add(failedKpi, 3, 0);
        VBox recent = card(); HBox title = sectionHeader("Tiến trình gần đây", "Theo dõi trực tiếp tiến độ, đạt/không đạt và lỗi mới nhất");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS); Button viewAll = secondaryButton("Xem tất cả  →");
        viewAll.setOnAction(event -> showPage("runs")); title.getChildren().addAll(spacer, viewAll);
        dashboardRuns = createRunTable(false); dashboardRuns.setPrefHeight(370); recent.getChildren().addAll(title, dashboardRuns);
        HBox flow = new HBox(16);
        VBox importCard = actionCard("01", "Nhập Excel", "Tải tệp có các sheet testcase tiếng Việt và kiểm tra cấu trúc trước khi chạy.");
        VBox runCard = actionCard("02", "Chạy như người dùng", "Trình duyệt thao tác click, điền dữ liệu và đối chiếu kết quả mong đợi.");
        VBox evidenceCard = actionCard("03", "Lưu bằng chứng", "Lưu ảnh, video, trace, log và báo cáo Excel sau mỗi lần chạy.");
        HBox.setHgrow(importCard, Priority.ALWAYS); HBox.setHgrow(runCard, Priority.ALWAYS); HBox.setHgrow(evidenceCard, Priority.ALWAYS); flow.getChildren().addAll(importCard, runCard, evidenceCard);
        content.getChildren().addAll(kpis, recent, flow); return scroll(content);
    }

    private Node buildProjects() {
        VBox content = page("projects");
        HBox heading = pageHeading("Dự án & chức năng", "Mỗi dự án có thư mục Excel, kết quả và thống kê riêng.");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS); Button newProject = primaryButton("＋  Tạo dự án");
        newProject.setOnAction(event -> createProjectDialog()); heading.getChildren().addAll(spacer, newProject);
        SplitPane split = new SplitPane(); split.getStyleClass().add("workspace-split"); split.setDividerPositions(0.31);
        VBox treeCard = card(); treeCard.setMinWidth(300);
        HBox treeHeader = sectionHeader("Cấu trúc kiểm thử", "Chọn dự án hoặc chức năng");
        Button addFeature = iconButton("＋"); addFeature.setTooltip(new Tooltip("Thêm chức năng vào dự án đang chọn")); addFeature.setOnAction(event -> createFeatureDialog());
        Region treeSpacer = new Region(); HBox.setHgrow(treeSpacer, Priority.ALWAYS); treeHeader.getChildren().addAll(treeSpacer, addFeature);
        projectTree = new TreeView<>(); projectTree.setShowRoot(false); projectTree.setCellFactory(tree -> new TreeCell<Object>() {
            @Override protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty); getStyleClass().removeAll("project-tree-item", "feature-tree-item");
                if (empty || item == null) { setText(null); setContextMenu(null); return; }
                if (item instanceof TestProject) {
                    TestProject project = (TestProject) item; setText("◇  " + project.name()); getStyleClass().add("project-tree-item");
                    MenuItem detail = new MenuItem("Xem tổng quan dự án"); detail.setOnAction(e -> showProjectOverview(project));
                    MenuItem delete = new MenuItem("Xóa dự án và tệp đi kèm"); delete.setOnAction(e -> deleteProject(project));
                    setContextMenu(new ContextMenu(detail, delete));
                } else if (item instanceof TestFeature) { setText("  └  " + ((TestFeature) item).name()); getStyleClass().add("feature-tree-item"); }
            }
        }); VBox.setVgrow(projectTree, Priority.ALWAYS);
        HBox treeActions = new HBox(8); Button overview = secondaryButton("Tổng quan dự án"); overview.setOnAction(e -> { if (selectedProject != null) showProjectOverview(selectedProject); });
        Button delete = dangerButton("Xóa dự án"); delete.setOnAction(e -> { if (selectedProject != null) deleteProject(selectedProject); }); treeActions.getChildren().addAll(overview, delete);
        treeCard.getChildren().addAll(treeHeader, projectTree, treeActions);

        VBox runCard = card(); HBox runHeader = sectionHeader("Tạo tiến trình kiểm thử", "Chọn chức năng, tệp Excel, tài khoản và chế độ chạy");
        selectedProjectLabel = valueLabel("Chưa chọn dự án"); selectedFeatureLabel = valueLabel("Chưa chọn chức năng"); GridPane selection = formGrid();
        selection.add(fieldLabel("Dự án"), 0, 0); selection.add(selectedProjectLabel, 1, 0); selection.add(fieldLabel("Chức năng"), 0, 1); selection.add(selectedFeatureLabel, 1, 1);
        VBox fileDrop = new VBox(8); fileDrop.getStyleClass().add("file-drop"); fileDrop.setAlignment(Pos.CENTER);
        Label fileIcon = new Label("⇧"); fileIcon.getStyleClass().add("file-icon"); Label fileTitle = new Label("Chọn tệp testcase Excel"); fileTitle.getStyleClass().add("file-title");
        excelFileLabel = new Label(".xlsx · có thể dùng nhiều sheet chức năng"); excelFileLabel.getStyleClass().add("muted"); Button browse = secondaryButton("Duyệt tệp"); browse.setOnAction(e -> chooseExcelFile());
        fileDrop.getChildren().addAll(fileIcon, fileTitle, excelFileLabel, browse);
        validationLabel = new Label("Chưa kiểm tra tệp"); validationLabel.getStyleClass().addAll("validation-message", "validation-neutral"); Button validate = secondaryButton("✓  Kiểm tra tệp"); validate.setOnAction(e -> validateExcel());
        usernameField = new TextField(); usernameField.setPromptText("${USERNAME} hoặc tài khoản kiểm thử"); passwordField = new PasswordField(); passwordField.setPromptText("${PASSWORD} hoặc mật khẩu kiểm thử");
        headlessCheck = new CheckBox("Chạy ẩn trình duyệt (không mở cửa sổ Chrome)"); headlessCheck.setSelected(false);
        GridPane credentials = formGrid(); credentials.add(fieldLabel("Tài khoản test"), 0, 0); credentials.add(usernameField, 1, 0); credentials.add(fieldLabel("Mật khẩu test"), 0, 1); credentials.add(passwordField, 1, 1); credentials.add(new Label(""), 0, 2); credentials.add(headlessCheck, 1, 2);
        HBox actions = new HBox(12, validate, validationLabel); actions.setAlignment(Pos.CENTER_LEFT); Button start = primaryButton("▶  CHẠY KIỂM THỬ"); start.getStyleClass().add("run-button"); start.setMaxWidth(Double.MAX_VALUE); start.setOnAction(e -> startRun());
        runCard.getChildren().addAll(runHeader, selection, separator(), fileDrop, actions, separator(), credentials, start); split.getItems().addAll(treeCard, runCard); content.getChildren().addAll(heading, split); VBox.setVgrow(split, Priority.ALWAYS); return content;
    }

    private Node buildRuns() {
        VBox content = page("runs"); content.getChildren().add(pageHeading("Tiến trình kiểm thử", "Lọc theo dự án/chức năng và xem đầy đủ lỗi, ảnh, video, trace, báo cáo."));
        HBox filters = new HBox(10); filters.getStyleClass().add("filter-bar");
        projectFilter = new ComboBox<>(); projectFilter.setPromptText("Tất cả dự án"); projectFilter.setPrefWidth(220);
        featureFilter = new ComboBox<>(); featureFilter.setPromptText("Tất cả chức năng"); featureFilter.setPrefWidth(220); Button clear = secondaryButton("Xóa bộ lọc"); clear.setOnAction(e -> { projectFilter.setValue(null); featureFilter.setValue(null); applyRunFilter(); });
        filters.getChildren().addAll(new Label("Lọc tiến trình:"), projectFilter, featureFilter, clear);
        SplitPane split = new SplitPane(); split.getStyleClass().add("workspace-split"); split.setDividerPositions(0.54);
        VBox tableCard = card(); HBox header = sectionHeader("Danh sách tiến trình", "Bấm một dòng để xem chi tiết"); Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS); Button refresh = secondaryButton("↻  Làm mới"); refresh.setOnAction(e -> refreshAll()); header.getChildren().addAll(spacer, refresh);
        filteredRuns = new FilteredList<>(controller.runs(), run -> true); runTable = createRunTable(true); runTable.setItems(filteredRuns); VBox.setVgrow(runTable, Priority.ALWAYS); tableCard.getChildren().addAll(header, filters, runTable);
        VBox detail = card(); detailTitle = new Label("Chọn một tiến trình"); detailTitle.getStyleClass().add("section-title"); detailMeta = new Label("Thông tin và bằng chứng sẽ hiển thị tại đây."); detailMeta.getStyleClass().add("muted");
        detailProgress = new ProgressBar(0); detailProgress.setMaxWidth(Double.MAX_VALUE); detailPercent = valueLabel("0%"); HBox progress = new HBox(12, detailProgress, detailPercent); progress.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(detailProgress, Priority.ALWAYS);
        detailStep = new Label("Chưa có bước nào"); detailStep.getStyleClass().add("current-step"); detailError = new Label(""); detailError.setWrapText(true); detailError.getStyleClass().add("error-text");
        StackPane previewFrame = new StackPane(); previewFrame.getStyleClass().add("preview-frame"); livePreview = new ImageView(); livePreview.setPreserveRatio(true); livePreview.setFitWidth(610); livePreview.setFitHeight(320);
        Label placeholder = new Label("XEM TRƯỚC TRỰC TIẾP\nẢnh trình duyệt sẽ cập nhật sau mỗi bước"); placeholder.setTextAlignment(javafx.scene.text.TextAlignment.CENTER); placeholder.getStyleClass().add("preview-placeholder"); livePreview.imageProperty().addListener((o, oldImage, newImage) -> placeholder.setVisible(newImage == null)); previewFrame.getChildren().addAll(placeholder, livePreview);
        stopButton = dangerButton("■  Dừng tiến trình"); stopButton.setTooltip(new Tooltip("Dừng tiến trình đang chạy")); stopButton.setOnAction(e -> stopSelectedRun());
        reportButton = secondaryButton("Xuất kết quả kiểm thử"); reportButton.setTooltip(new Tooltip("Mở tệp Excel kết quả")); reportButton.setOnAction(e -> openArtifact("test-results.xlsx"));
        videoButton = secondaryButton("Mở video kiểm thử"); videoButton.setTooltip(new Tooltip("Mở video thao tác trên trình duyệt")); videoButton.setOnAction(e -> openArtifact("run-video.webm"));
        traceButton = secondaryButton("Mở trace chẩn đoán"); traceButton.setTooltip(new Tooltip("Mở trace để xem lại từng bước và lỗi")); traceButton.setOnAction(e -> openArtifact("trace.zip"));
        folderButton = secondaryButton("Mở thư mục dự án"); folderButton.setTooltip(new Tooltip("Mở thư mục chứa Excel và toàn bộ kết quả")); folderButton.setOnAction(e -> openArtifact(""));
        HBox artifactButtons = new HBox(8, stopButton, reportButton, videoButton, traceButton, folderButton);
        errorList = new ListView<>(); errorList.setPlaceholder(new Label("Chưa có lỗi nào. Các lỗi của lần chạy sẽ hiển thị ở đây.")); errorList.setPrefHeight(130); errorList.getStyleClass().add("error-list");
        VBox errors = card(); errors.setPadding(new Insets(14)); errors.getChildren().addAll(sectionHeader("Lỗi kiểm thử", "Danh sách lỗi để kiểm tra nhanh"), errorList);
        detail.getChildren().addAll(detailTitle, detailMeta, progress, detailStep, detailError, previewFrame, artifactButtons, errors); VBox.setVgrow(previewFrame, Priority.ALWAYS);
        split.getItems().addAll(tableCard, detail); content.getChildren().add(split); VBox.setVgrow(split, Priority.ALWAYS); return content;
    }

    private Node buildSettings() {
        VBox content = page("settings"); content.getChildren().add(pageHeading("Cấu hình", "Các giá trị có thể chỉnh sửa và lưu vào config/application.properties."));
        VBox editor = card(); editor.getChildren().add(sectionHeader("Thiết lập runner", "Chỉ mật khẩu không được ghi vào file cấu hình")); GridPane form = formGrid();
        configUrlField = new TextField(controller.config().get("env.BASE_URL", "")); configUsernameField = new TextField(controller.config().get("env.USERNAME", "")); configPasswordField = new PasswordField(); configPasswordField.setPromptText("Không ghi vào file cấu hình");
        configHeadlessCheck = new CheckBox("Chạy ẩn trình duyệt theo mặc định"); configHeadlessCheck.setSelected(controller.config().getBoolean("runner.headless", false)); configTimeoutField = new TextField(Integer.toString(controller.config().getInt("runner.defaultTimeoutMs", 15000)));
        form.add(fieldLabel("URL mặc định"), 0, 0); form.add(configUrlField, 1, 0); form.add(fieldLabel("Tài khoản mặc định"), 0, 1); form.add(configUsernameField, 1, 1); form.add(fieldLabel("Mật khẩu phiên này"), 0, 2); form.add(configPasswordField, 1, 2); form.add(fieldLabel("Thời gian chờ (ms)"), 0, 3); form.add(configTimeoutField, 1, 3); form.add(new Label(""), 0, 4); form.add(configHeadlessCheck, 1, 4);
        Button save = primaryButton("Lưu cấu hình"); save.setOnAction(e -> saveSettings()); editor.getChildren().addAll(form, save);
        VBox guide = card(); guide.getChildren().addAll(sectionHeader("Cách dùng", "Các khóa thao tác trong Excel vẫn giữ tiếng Anh để runner nhận diện."), bullet("URL, tài khoản và thời gian chờ có thể sửa trực tiếp rồi bấm Lưu cấu hình."), bullet("Mật khẩu chỉ dùng trong phiên chạy, không ghi vào application.properties."), bullet("Trace là gói chẩn đoán để xem lại DOM, ảnh và từng bước khi lỗi."), bullet("Mỗi dự án có thư mục riêng chứa file Excel và kết quả; xóa dự án sẽ xóa cả thư mục đó."));
        VBox actions = card(); actions.getChildren().addAll(sectionHeader("Khóa thao tác trong Excel", "Chỉ các giá trị kỹ thuật này dùng tiếng Anh"), wrapChips("goto", "click", "fill", "press", "select", "check", "uncheck", "upload", "wait", "expectText", "expectVisible", "expectHidden", "expectUrl", "expectRowsContain", "screenshot")); content.getChildren().addAll(editor, guide, actions); return scroll(content);
    }

    private void registerEvents() {
        controller.runs().addListener((ListChangeListener<TestRun>) change -> { refreshMetrics(); refreshFilters(); refreshErrorList(); });
        controller.onRunUpdate(run -> { refreshMetrics(); refreshFilters(); refreshErrorList(); dashboardRuns.refresh(); runTable.refresh(); if (isSelected(run)) showRunDetail(run); });
        controller.onPreview((run, screenshot) -> { previews.put(run.id(), screenshot); if (isSelected(run)) setPreview(screenshot); });
        controller.onCompleted(summary -> { refreshMetrics(); refreshFilters(); refreshErrorList(); if (isSelected(summary.run())) showRunDetail(summary.run()); });
        projectTree.getSelectionModel().selectedItemProperty().addListener((o, oldItem, newItem) -> {
            if (newItem == null) return; Object value = newItem.getValue();
            if (value instanceof TestProject) { selectedProject = (TestProject) value; selectedFeature = null; }
            else if (value instanceof TestFeature) { selectedFeature = (TestFeature) value; TreeItem<Object> parent = newItem.getParent(); selectedProject = parent != null && parent.getValue() instanceof TestProject ? (TestProject) parent.getValue() : null; }
            refreshSelectionLabels();
        });
        runTable.getSelectionModel().selectedItemProperty().addListener((o, oldRun, run) -> showRunDetail(run));
        projectFilter.valueProperty().addListener((o, oldValue, newValue) -> { refreshFeatureFilter(); applyRunFilter(); });
        featureFilter.valueProperty().addListener((o, oldValue, newValue) -> applyRunFilter());
    }

    private void refreshAll() {
        controller.reload(); refreshProjectTree(); if (dashboardRuns != null) dashboardRuns.setItems(controller.runs()); if (runTable != null) runTable.setItems(filteredRuns); refreshMetrics(); refreshFilters(); refreshErrorList();
        if (runTable != null && !controller.runs().isEmpty() && runTable.getSelectionModel().getSelectedItem() == null) runTable.getSelectionModel().selectFirst();
    }

    private void refreshMetrics() {
        long running = controller.runs().stream().filter(run -> run.status() == RunStatus.RUNNING || run.status() == RunStatus.QUEUED).count();
        long passed = controller.runs().stream().filter(run -> run.status() == RunStatus.PASSED).count(); long failed = controller.runs().stream().filter(run -> run.status() == RunStatus.FAILED).count();
        projectKpi.setValue(Integer.toString(controller.projects().size())); projectKpi.setCaption("hệ thống đã cấu hình"); runningKpi.setValue(Long.toString(running)); runningKpi.setCaption(running > 0 ? "đang thao tác trên trình duyệt" : "runner sẵn sàng"); passedKpi.setValue(Long.toString(passed)); passedKpi.setCaption("tiến trình hoàn thành tốt"); failedKpi.setValue(Long.toString(failed)); failedKpi.setCaption("tiến trình có lỗi");
    }

    private void refreshProjectTree() {
        TreeItem<Object> root = new TreeItem<>("root"); root.setExpanded(true); for (TestProject project : controller.projects()) { TreeItem<Object> projectItem = new TreeItem<>(project); projectItem.setExpanded(true); controller.features(project.id()).forEach(feature -> projectItem.getChildren().add(new TreeItem<>(feature))); root.getChildren().add(projectItem); } projectTree.setRoot(root);
    }

    private void refreshFilters() {
        if (projectFilter == null) return; String current = projectFilter.getValue(); projectFilter.setItems(FXCollections.observableArrayList(controller.projects().stream().map(TestProject::name).collect(Collectors.toList()))); if (current != null && projectFilter.getItems().contains(current)) projectFilter.setValue(current); refreshFeatureFilter(); applyRunFilter();
    }

    private void refreshFeatureFilter() {
        if (featureFilter == null) return; String current = featureFilter.getValue(); String selectedProject = projectFilter.getValue(); List<String> names = controller.projects().stream().filter(project -> selectedProject == null || selectedProject.equals(project.name())).flatMap(project -> controller.features(project.id()).stream()).map(TestFeature::name).distinct().collect(Collectors.toList()); featureFilter.setItems(FXCollections.observableArrayList(names)); if (current != null && names.contains(current)) featureFilter.setValue(current); else if (current != null) featureFilter.setValue(null);
    }

    private void applyRunFilter() {
        if (filteredRuns == null) return; String project = projectFilter == null ? null : projectFilter.getValue(); String feature = featureFilter == null ? null : featureFilter.getValue(); filteredRuns.setPredicate(run -> (project == null || project.equals(run.projectName())) && (feature == null || feature.equals(run.featureName())));
    }

    private void refreshErrorList() {
        if (errorList == null) return; errorList.getItems().setAll(controller.runs().stream().filter(run -> run.errorMessage() != null && !run.errorMessage().isBlank()).map(run -> DATE_TIME.format(run.startedAt()) + " · " + run.projectName() + " / " + run.featureName() + "\n" + run.errorMessage()).collect(Collectors.toList()));
    }

    private void createProjectDialog() {
        Dialog<ButtonType> dialog = formDialog("Tạo dự án mới", "Dự án đại diện cho website hoặc hệ thống cần kiểm thử."); TextField name = new TextField(); name.setPromptText("Ví dụ: Cổng thông tin nội bộ"); TextField url = new TextField(); url.setPromptText("https://staging.example.com"); TextArea description = new TextArea(); description.setPromptText("Mô tả ngắn về hệ thống..."); description.setPrefRowCount(3); GridPane form = formGrid(); form.add(fieldLabel("Tên dự án"), 0, 0); form.add(name, 1, 0); form.add(fieldLabel("URL cơ sở"), 0, 1); form.add(url, 1, 1); form.add(fieldLabel("Mô tả"), 0, 2); form.add(description, 1, 2); dialog.getDialogPane().setContent(form);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) try { TestProject created = controller.createProject(name.getText(), description.getText(), url.getText()); refreshProjectTree(); selectTreeValue(created); showMessage(Alert.AlertType.INFORMATION, "Đã tạo dự án", "Bạn có thể thêm chức năng và nhập testcase ngay bây giờ."); } catch (RuntimeException error) { showError(error); }
    }

    private void createFeatureDialog() {
        if (selectedProject == null) { showMessage(Alert.AlertType.WARNING, "Chưa chọn dự án", "Hãy chọn một dự án trước khi tạo chức năng."); return; }
        Dialog<ButtonType> dialog = formDialog("Thêm chức năng", "Dự án: " + selectedProject.name()); TextField name = new TextField(); name.setPromptText("Ví dụ: Danh sách bán hàng"); TextArea description = new TextArea(); description.setPrefRowCount(3); GridPane form = formGrid(); form.add(fieldLabel("Tên chức năng"), 0, 0); form.add(name, 1, 0); form.add(fieldLabel("Mô tả"), 0, 1); form.add(description, 1, 1); dialog.getDialogPane().setContent(form);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) try { TestFeature created = controller.createFeature(selectedProject, name.getText(), description.getText()); refreshProjectTree(); selectTreeValue(created); } catch (RuntimeException error) { showError(error); }
    }

    private void deleteProject(TestProject project) {
        boolean running = controller.runs().stream().anyMatch(run -> run.projectId() == project.id()
                && (run.status() == RunStatus.RUNNING || run.status() == RunStatus.QUEUED));
        if (running) {
            showMessage(Alert.AlertType.WARNING, "Dự án đang có tiến trình", "Hãy dừng hoặc chờ các tiến trình của dự án hoàn tất trước khi xóa.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION); confirm.initOwner(stage); confirm.setTitle("Xóa dự án"); confirm.setHeaderText("Xóa dự án và toàn bộ dữ liệu đi kèm?"); confirm.setContentText("Các chức năng, file Excel, video, trace, log và báo cáo của dự án sẽ bị xóa.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return; try { controller.deleteProject(project); selectedProject = null; selectedFeature = null; refreshProjectTree(); refreshSelectionLabels(); showMessage(Alert.AlertType.INFORMATION, "Đã xóa dự án", "Đã xóa cả thư mục dữ liệu của dự án."); } catch (RuntimeException error) { showError(error); }
    }

    private void showProjectOverview(TestProject project) {
        if (project == null) return; Dialog<ButtonType> dialog = formDialog("Tổng quan dự án: " + project.name(), project.baseUrl().isBlank() ? "" : project.baseUrl()); dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
        List<TestRun> runs = controller.runs().stream().filter(run -> run.projectId() == project.id()).collect(Collectors.toList()); long passed = runs.stream().filter(run -> run.status() == RunStatus.PASSED).count(); long failed = runs.stream().filter(run -> run.status() == RunStatus.FAILED).count(); long running = runs.stream().filter(run -> run.status() == RunStatus.RUNNING || run.status() == RunStatus.QUEUED).count();
        GridPane kpis = new GridPane(); kpis.setHgap(10); kpis.add(new KpiCard("✓", "Đạt", "accent-green"), 0, 0); kpis.add(new KpiCard("!", "Không đạt", "accent-red"), 1, 0); kpis.add(new KpiCard("▶", "Đang chạy", "accent-indigo"), 2, 0); ((KpiCard) kpis.getChildren().get(0)).setValue(Long.toString(passed)); ((KpiCard) kpis.getChildren().get(1)).setValue(Long.toString(failed)); ((KpiCard) kpis.getChildren().get(2)).setValue(Long.toString(running));
        TableView<TestFeature> table = new TableView<>(); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); table.setItems(FXCollections.observableArrayList(controller.features(project.id()))); TableColumn<TestFeature, String> feature = new TableColumn<>("Chức năng"); feature.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().name())); TableColumn<TestFeature, Number> total = new TableColumn<>("Tổng lần chạy"); total.setCellValueFactory(data -> new ReadOnlyIntegerWrapper((int) runs.stream().filter(run -> run.featureId() == data.getValue().id()).count())); TableColumn<TestFeature, Number> ok = new TableColumn<>("Đạt"); ok.setCellValueFactory(data -> new ReadOnlyIntegerWrapper((int) runs.stream().filter(run -> run.featureId() == data.getValue().id() && run.status() == RunStatus.PASSED).count())); TableColumn<TestFeature, Number> bad = new TableColumn<>("Không đạt"); bad.setCellValueFactory(data -> new ReadOnlyIntegerWrapper((int) runs.stream().filter(run -> run.featureId() == data.getValue().id() && run.status() == RunStatus.FAILED).count())); table.getColumns().addAll(feature, total, ok, bad); table.setPrefHeight(300);
        VBox box = new VBox(14, kpis, sectionHeader("Thống kê theo chức năng", "Mỗi dòng là một chức năng trong dự án"), table); dialog.getDialogPane().setContent(box); dialog.getDialogPane().setPrefWidth(780); dialog.showAndWait();
    }

    private void chooseExcelFile() { FileChooser chooser = new FileChooser(); chooser.setTitle("Chọn tệp testcase Excel"); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tệp Excel", "*.xlsx")); java.io.File file = chooser.showOpenDialog(stage); if (file == null) return; selectedExcelFile = file.toPath(); excelFileLabel.setText(file.getName()); setValidation("Chưa kiểm tra tệp mới", false, false); }

    private void validateExcel() { if (selectedExcelFile == null) { showMessage(Alert.AlertType.WARNING, "Chưa có tệp", "Hãy chọn tệp Excel trước."); return; } try { ImportResult result = controller.validateExcel(selectedExcelFile, selectedFeature == null ? null : selectedFeature.name()); setValidation("Hợp lệ · " + result.testCaseCount() + " testcase · " + result.steps().size() + " bước", true, false); } catch (RuntimeException error) { setValidation("Không hợp lệ · " + error.getMessage(), false, true); } }

    private void startRun() { try { if (selectedProject == null) throw new IllegalArgumentException("Hãy chọn dự án"); if (selectedFeature == null) throw new IllegalArgumentException("Hãy chọn chức năng"); if (selectedExcelFile == null) throw new IllegalArgumentException("Hãy chọn tệp Excel"); controller.validateExcel(selectedExcelFile, selectedFeature.name()); TestRun run = controller.startRun(selectedProject, selectedFeature, selectedExcelFile, headlessCheck.isSelected(), usernameField.getText(), passwordField.getText()); passwordField.clear(); showPage("runs"); runTable.getSelectionModel().select(run); showRunDetail(run); } catch (RuntimeException error) { showError(error); } }

    private void stopSelectedRun() { TestRun run = runTable.getSelectionModel().getSelectedItem(); if (run == null) return; if (!controller.cancel(run.id())) showMessage(Alert.AlertType.INFORMATION, "Không có tiến trình đang chạy", "Tiến trình này đã kết thúc."); }

    private void showRunDetail(TestRun run) { if (run == null || detailTitle == null) return; detailTitle.setText(run.projectName() + "  /  " + run.featureName()); detailMeta.setText(DATE_TIME.format(run.startedAt()) + "  ·  " + Path.of(run.sourceFile()).getFileName() + "  ·  " + statusText(run.status())); detailProgress.setProgress(run.progress() / 100.0); detailPercent.setText(run.progress() + "%"); detailStep.setText(run.currentStep() == null || run.currentStep().isBlank() ? "Chưa có bước nào" : run.currentStep()); detailError.setText(run.errorMessage() == null ? "" : run.errorMessage()); stopButton.setDisable(run.status() != RunStatus.RUNNING && run.status() != RunStatus.QUEUED); reportButton.setDisable(!Files.exists(run.artifactDirectory().resolve("test-results.xlsx"))); videoButton.setDisable(!Files.exists(run.artifactDirectory().resolve("run-video.webm"))); traceButton.setDisable(!Files.exists(run.artifactDirectory().resolve("trace.zip"))); folderButton.setDisable(!Files.exists(run.artifactDirectory())); Path preview = previews.get(run.id()); if (preview == null) preview = latestScreenshot(run.artifactDirectory().resolve("screenshots")); if (preview != null) setPreview(preview); else livePreview.setImage(null); }

    private static String statusText(RunStatus status) { switch (status) { case QUEUED: return "Đang chờ"; case RUNNING: return "Đang chạy"; case PASSED: return "Đạt"; case FAILED: return "Không đạt"; case CANCELLED: return "Đã dừng"; default: return status.name(); } }
    private void saveSettings() { try { int timeout = Integer.parseInt(configTimeoutField.getText().trim()); if (timeout < 100) throw new NumberFormatException(); controller.config().set("env.BASE_URL", configUrlField.getText().trim()); controller.config().set("env.USERNAME", configUsernameField.getText().trim()); controller.config().set("runner.headless", Boolean.toString(configHeadlessCheck.isSelected())); controller.config().set("runner.defaultTimeoutMs", Integer.toString(timeout)); controller.config().save(); usernameField.setText(configUsernameField.getText()); passwordField.setText(configPasswordField.getText()); configPasswordField.clear(); headlessCheck.setSelected(configHeadlessCheck.isSelected()); showMessage(Alert.AlertType.INFORMATION, "Đã lưu cấu hình", "Cấu hình sẽ được áp dụng cho các lần chạy tiếp theo. Mật khẩu chỉ được áp dụng cho phiên này."); } catch (NumberFormatException error) { showMessage(Alert.AlertType.ERROR, "Thời gian chờ không hợp lệ", "Hãy nhập số mili-giây lớn hơn hoặc bằng 100."); } catch (RuntimeException error) { showError(error); } }

    private void openArtifact(String filename) { TestRun run = runTable.getSelectionModel().getSelectedItem(); if (run == null) return; Path path = filename.isBlank() ? run.artifactDirectory().getParent().getParent() : run.artifactDirectory().resolve(filename); if (!Files.exists(path)) { showMessage(Alert.AlertType.WARNING, "Chưa có tệp", "Tệp này chỉ có sau khi tiến trình hoàn tất."); return; } try { Desktop.getDesktop().open(path.toFile()); } catch (IOException | UnsupportedOperationException error) { showMessage(Alert.AlertType.ERROR, "Không mở được tệp", path.toAbsolutePath().toString()); } }

    private TableView<TestRun> createRunTable(boolean full) { TableView<TestRun> table = new TableView<>(controller.runs()); table.getStyleClass().add("run-table"); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); table.setPlaceholder(new Label("Chưa có tiến trình. Hãy tạo dự án và chạy testcase đầu tiên.")); TableColumn<TestRun, String> time = new TableColumn<>("BẮT ĐẦU"); time.setCellValueFactory(data -> new ReadOnlyStringWrapper(DATE_TIME.format(data.getValue().startedAt()))); TableColumn<TestRun, String> name = new TableColumn<>("DỰ ÁN / CHỨC NĂNG"); name.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().projectName() + "\n" + data.getValue().featureName())); TableColumn<TestRun, RunStatus> status = new TableColumn<>("TRẠNG THÁI"); status.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().status())); status.setCellFactory(column -> new TableCell<TestRun, RunStatus>() { private final StatusBadge badge = new StatusBadge(); @Override protected void updateItem(RunStatus item, boolean empty) { super.updateItem(item, empty); if (empty || item == null) setGraphic(null); else { badge.setStatus(item); setGraphic(badge); } } }); TableColumn<TestRun, Number> progress = new TableColumn<>("TIẾN ĐỘ"); progress.setCellValueFactory(data -> new ReadOnlyIntegerWrapper(data.getValue().progress())); progress.setCellFactory(column -> new TableCell<TestRun, Number>() { private final ProgressBar bar = new ProgressBar(); private final Label percent = new Label(); private final HBox box = new HBox(6, bar, percent); { box.setAlignment(Pos.CENTER_LEFT); bar.setPrefWidth(82); } @Override protected void updateItem(Number item, boolean empty) { super.updateItem(item, empty); if (empty || item == null) setGraphic(null); else { bar.setProgress(item.doubleValue() / 100.0); percent.setText(item.intValue() + "%"); setGraphic(box); } } }); TableColumn<TestRun, String> score = new TableColumn<>("KẾT QUẢ"); score.setCellValueFactory(data -> new ReadOnlyStringWrapper("✓ " + data.getValue().passedSteps() + "   ✕ " + data.getValue().failedSteps())); table.getColumns().addAll(time, name, status, progress, score); if (full) { TableColumn<TestRun, String> file = new TableColumn<>("TỆP EXCEL"); file.setCellValueFactory(data -> new ReadOnlyStringWrapper(Path.of(data.getValue().sourceFile()).getFileName().toString())); table.getColumns().add(file); } return table; }

    private void setPreview(Path screenshot) { if (screenshot != null && Files.exists(screenshot)) livePreview.setImage(new Image(screenshot.toUri().toString(), false)); }
    private static Path latestScreenshot(Path directory) { if (!Files.isDirectory(directory)) return null; try (java.util.stream.Stream<Path> paths = Files.list(directory)) { return paths.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png")).max(Comparator.comparing(path -> path.getFileName().toString())).orElse(null); } catch (IOException ignored) { return null; } }
    private Button navButton(String id, String icon, String text) { Button button = new Button(icon + "   " + text); button.getStyleClass().add("nav-button"); button.setMaxWidth(Double.MAX_VALUE); button.setAlignment(Pos.CENTER_LEFT); button.setOnAction(e -> showPage(id)); navigation.put(id, button); return button; }
    private void showPage(String id) { pages.getChildren().forEach(node -> node.setVisible(id.equals(node.getUserData()))); navigation.forEach((key, button) -> button.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), key.equals(id))); }
    private static VBox page(String id) { VBox page = new VBox(20); page.setUserData(id); page.getStyleClass().add("page"); page.setPadding(new Insets(28, 30, 30, 30)); return page; }
    private static ScrollPane scroll(Node content) { ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true); scroll.getStyleClass().add("page-scroll"); scroll.setUserData(content.getUserData()); return scroll; }
    private static HBox pageHeading(String title, String subtitle) { HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT); VBox text = new VBox(4); Label titleLabel = new Label(title); titleLabel.getStyleClass().add("page-title"); Label subtitleLabel = new Label(subtitle); subtitleLabel.getStyleClass().add("page-subtitle"); text.getChildren().addAll(titleLabel, subtitleLabel); row.getChildren().add(text); return row; }
    private static HBox sectionHeader(String title, String subtitle) { HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT); VBox text = new VBox(3); Label titleLabel = new Label(title); titleLabel.getStyleClass().add("section-title"); Label subtitleLabel = new Label(subtitle); subtitleLabel.getStyleClass().add("muted"); text.getChildren().addAll(titleLabel, subtitleLabel); row.getChildren().add(text); return row; }
    private static VBox card() { VBox card = new VBox(16); card.getStyleClass().add("card"); card.setPadding(new Insets(22)); return card; }
    private static VBox actionCard(String number, String title, String text) { VBox card = card(); Label index = new Label(number); index.getStyleClass().add("flow-number"); Label titleLabel = new Label(title); titleLabel.getStyleClass().add("section-title"); Label textLabel = new Label(text); textLabel.setWrapText(true); textLabel.getStyleClass().add("muted"); card.getChildren().addAll(index, titleLabel, textLabel); return card; }
    private static Button primaryButton(String text) { Button button = new Button(text); button.getStyleClass().add("primary-button"); return button; }
    private static Button secondaryButton(String text) { Button button = new Button(text); button.getStyleClass().add("secondary-button"); return button; }
    private static Button dangerButton(String text) { Button button = new Button(text); button.getStyleClass().add("danger-button"); return button; }
    private static Button iconButton(String text) { Button button = new Button(text); button.getStyleClass().add("icon-button"); return button; }
    private static Label fieldLabel(String text) { Label label = new Label(text); label.getStyleClass().add("field-label"); return label; }
    private static Label valueLabel(String text) { Label label = new Label(text); label.getStyleClass().add("value-label"); return label; }
    private static GridPane formGrid() { GridPane grid = new GridPane(); grid.setHgap(16); grid.setVgap(13); ColumnConstraints labels = new ColumnConstraints(150); ColumnConstraints inputs = new ColumnConstraints(); inputs.setHgrow(Priority.ALWAYS); inputs.setFillWidth(true); grid.getColumnConstraints().addAll(labels, inputs); return grid; }
    private static Separator separator() { Separator separator = new Separator(); separator.getStyleClass().add("soft-separator"); return separator; }
    private static Dialog<ButtonType> formDialog(String title, String subtitle) { Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle(title); dialog.setHeaderText(subtitle); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL); dialog.getDialogPane().setPrefWidth(560); return dialog; }
    private static Label bullet(String text) { Label label = new Label("✓  " + text); label.getStyleClass().add("settings-bullet"); label.setWrapText(true); return label; }
    private static FlowPane wrapChips(String... values) { FlowPane pane = new FlowPane(8, 8); for (String value : values) { Label chip = new Label(value); chip.getStyleClass().add("action-chip"); pane.getChildren().add(chip); } return pane; }
    private static ColumnConstraints cloneColumn(ColumnConstraints source) { ColumnConstraints copy = new ColumnConstraints(); copy.setPercentWidth(source.getPercentWidth()); return copy; }
    private void setValidation(String text, boolean success, boolean error) { validationLabel.setText(text); validationLabel.getStyleClass().removeAll("validation-neutral", "validation-success", "validation-error"); validationLabel.getStyleClass().add(success ? "validation-success" : error ? "validation-error" : "validation-neutral"); }
    private void refreshSelectionLabels() { selectedProjectLabel.setText(selectedProject == null ? "Chưa chọn dự án" : selectedProject.name()); selectedFeatureLabel.setText(selectedFeature == null ? "Chưa chọn chức năng" : selectedFeature.name()); }
    private void selectTreeValue(Object value) { for (TreeItem<Object> projectItem : projectTree.getRoot().getChildren()) { if (sameEntity(projectItem.getValue(), value)) { projectTree.getSelectionModel().select(projectItem); return; } for (TreeItem<Object> featureItem : projectItem.getChildren()) if (sameEntity(featureItem.getValue(), value)) { projectTree.getSelectionModel().select(featureItem); return; } } }
    private static boolean sameEntity(Object left, Object right) { if (left instanceof TestProject && right instanceof TestProject) return ((TestProject) left).id() == ((TestProject) right).id(); if (left instanceof TestFeature && right instanceof TestFeature) return ((TestFeature) left).id() == ((TestFeature) right).id(); return Objects.equals(left, right); }
    private boolean isSelected(TestRun run) { return runTable != null && runTable.getSelectionModel().getSelectedItem() != null && runTable.getSelectionModel().getSelectedItem().id().equals(run.id()); }
    private void showError(Throwable error) { Throwable current = error; while (current.getCause() != null) current = current.getCause(); showMessage(Alert.AlertType.ERROR, "Không thể thực hiện", current.getMessage()); }
    private void showMessage(Alert.AlertType type, String title, String message) { Alert alert = new Alert(type); alert.initOwner(stage); alert.setTitle("TestPilot Studio"); alert.setHeaderText(title); alert.setContentText(message == null ? "Lỗi không xác định" : message); alert.showAndWait(); }
}

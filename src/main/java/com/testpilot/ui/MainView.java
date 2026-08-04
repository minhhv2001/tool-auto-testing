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
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    private TextField excelStartRowField;
    private TextField excelStartColumnField;
    private Path selectedExcelFile;
    private TestProject selectedProject;
    private TestFeature selectedFeature;
    private TextField configUrlField;
    private TextField configUsernameField;
    private PasswordField configPasswordField;
    private CheckBox configHeadlessCheck;
    private TextField configTimeoutField;
    private TextField configTemplateField;
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
        Label name = new Label("AUTO TESTING IMD"); name.getStyleClass().add("brand-name");
        Label studio = new Label("KIỂM THỬ TỰ ĐỘNG"); studio.getStyleClass().add("brand-subtitle");
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
        Label tipText = new Label("Ưu tiên cột Đối tượng theo data-testid để testcase bền vững khi giao diện thay đổi.");
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
            {
                setMaxWidth(Double.MAX_VALUE);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty); getStyleClass().removeAll("project-tree-item", "feature-tree-item");
                if (empty || item == null) { setText(null); setGraphic(null); setContextMenu(null); return; }
                if (item instanceof TestProject) {
                    TestProject project = (TestProject) item; getStyleClass().add("project-tree-item");
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    Label name = new Label("◇  " + project.name()); name.getStyleClass().add("tree-project-name"); name.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(name, Priority.ALWAYS);
                    Region rowSpacer = new Region(); HBox.setHgrow(rowSpacer, Priority.ALWAYS);
                    Button detail = treeRowButton("▤"); detail.setTooltip(new Tooltip("Xem tổng quan dự án")); detail.setOnAction(e -> showProjectOverview(project));
                    Button delete = treeRowButton("×"); delete.getStyleClass().add("tree-delete-button"); delete.setTooltip(new Tooltip("Xóa dự án và toàn bộ tệp đi kèm")); delete.setOnAction(e -> deleteProject(project));
                    HBox row = new HBox(8, name, rowSpacer, detail, delete); row.setAlignment(Pos.CENTER_LEFT); row.setMaxWidth(Double.MAX_VALUE); row.setFillHeight(true);
                    row.prefWidthProperty().bind(tree.widthProperty().subtract(52)); setText(null); setGraphic(row); setContextMenu(null);
                } else if (item instanceof TestFeature) { setContentDisplay(ContentDisplay.TEXT_ONLY); setText("  └  " + ((TestFeature) item).name()); setGraphic(null); getStyleClass().add("feature-tree-item"); }
            }
        }); VBox.setVgrow(projectTree, Priority.ALWAYS);
        treeCard.getChildren().addAll(treeHeader, projectTree);

        VBox runCard = card(); HBox runHeader = sectionHeader("Tạo tiến trình kiểm thử", "Chọn chức năng, tệp Excel, tài khoản và chế độ chạy");
        selectedProjectLabel = valueLabel("Chưa chọn dự án"); selectedFeatureLabel = valueLabel("Chưa chọn chức năng"); GridPane selection = formGrid();
        selection.add(fieldLabel("Dự án"), 0, 0); selection.add(selectedProjectLabel, 1, 0); selection.add(fieldLabel("Chức năng"), 0, 1); selection.add(selectedFeatureLabel, 1, 1);
        VBox fileDrop = new VBox(8); fileDrop.getStyleClass().add("file-drop"); fileDrop.setAlignment(Pos.CENTER);
        Label fileIcon = new Label("⇧"); fileIcon.getStyleClass().add("file-icon"); Label fileTitle = new Label("Kéo thả tệp testcase Excel vào đây"); fileTitle.getStyleClass().add("file-title");
        excelFileLabel = new Label(".xlsx · hoặc dùng các nút bên dưới"); excelFileLabel.getStyleClass().add("muted"); Button browse = secondaryButton("Chọn tệp Excel"); browse.setOnAction(e -> chooseExcelFile());
        Button template = secondaryButton("Tải mẫu testcase"); template.setOnAction(e -> downloadTestCaseTemplate());
        HBox fileActions = new HBox(9, browse, template); fileActions.setAlignment(Pos.CENTER);
        fileDrop.getChildren().addAll(fileIcon, fileTitle, excelFileLabel, fileActions);
        fileDrop.setOnDragOver(event -> { Dragboard board = event.getDragboard(); if (board.hasFiles() && board.getFiles().stream().anyMatch(file -> isExcelFile(file.toPath()))) event.acceptTransferModes(TransferMode.COPY); event.consume(); });
        fileDrop.setOnDragDropped(event -> { boolean completed = false; for (java.io.File file : event.getDragboard().getFiles()) { if (isExcelFile(file.toPath())) { selectExcelFile(file.toPath()); completed = true; break; } } event.setDropCompleted(completed); event.consume(); });
        excelStartRowField = new TextField("1"); excelStartRowField.setPromptText("VD: 1");
        excelStartColumnField = new TextField("A"); excelStartColumnField.setPromptText("VD: A hoặc 1");
        GridPane readOptions = formGrid();
        readOptions.add(fieldLabel("Dòng header"), 0, 0); readOptions.add(excelStartRowField, 1, 0);
        readOptions.add(fieldLabel("Cột bắt đầu"), 0, 1); readOptions.add(excelStartColumnField, 1, 1);
        validationLabel = new Label("Chưa kiểm tra tệp"); validationLabel.getStyleClass().addAll("validation-message", "validation-neutral"); Button validate = secondaryButton("✓  Kiểm tra tệp"); validate.setOnAction(e -> validateExcel());
        usernameField = new TextField(); usernameField.setPromptText("${USERNAME} hoặc tài khoản kiểm thử"); passwordField = new PasswordField(); passwordField.setPromptText("${PASSWORD} hoặc mật khẩu kiểm thử");
        headlessCheck = new CheckBox("Chạy ẩn trình duyệt (không mở cửa sổ Chrome)"); headlessCheck.setSelected(false);
        GridPane credentials = formGrid(); credentials.add(fieldLabel("Tài khoản test"), 0, 0); credentials.add(usernameField, 1, 0); credentials.add(fieldLabel("Mật khẩu test"), 0, 1); credentials.add(passwordField, 1, 1); credentials.add(new Label(""), 0, 2); credentials.add(headlessCheck, 1, 2);
        HBox actions = new HBox(12, validate, validationLabel); actions.setAlignment(Pos.CENTER_LEFT); Button start = primaryButton("▶  CHẠY KIỂM THỬ"); start.getStyleClass().add("run-button"); start.setMaxWidth(Double.MAX_VALUE); start.setOnAction(e -> startRun());
        split.setPrefHeight(720);
        runCard.getChildren().addAll(runHeader, selection, separator(), fileDrop, readOptions, actions, separator(), credentials, start); split.getItems().addAll(treeCard, runCard); content.getChildren().addAll(heading, split); VBox.setVgrow(split, Priority.ALWAYS); return scroll(content);
    }

    private Node buildRuns() {
        VBox content = page("runs"); content.getChildren().add(pageHeading("Tiến trình kiểm thử", "Lọc theo dự án/chức năng và xem đầy đủ lỗi, ảnh, video, trace, báo cáo."));
        FlowPane filters = new FlowPane(10, 10); filters.getStyleClass().add("filter-bar");
        projectFilter = new ComboBox<>(); projectFilter.setPromptText("Tất cả dự án"); projectFilter.setPrefWidth(220);
        featureFilter = new ComboBox<>(); featureFilter.setPromptText("Tất cả chức năng"); featureFilter.setPrefWidth(220); Button clear = secondaryButton("Xóa bộ lọc"); clear.setOnAction(e -> { projectFilter.setValue(null); featureFilter.setValue(null); applyRunFilter(); });
        filters.getChildren().addAll(new Label("Lọc tiến trình:"), projectFilter, featureFilter, clear);
        SplitPane split = new SplitPane(); split.getStyleClass().add("workspace-split"); split.setDividerPositions(0.54);
        VBox tableCard = card(); HBox header = sectionHeader("Danh sách tiến trình", "Bấm một dòng để xem chi tiết"); Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS); Button refresh = secondaryButton("↻  Làm mới"); refresh.setOnAction(e -> refreshAll()); header.getChildren().addAll(spacer, refresh);
        filteredRuns = new FilteredList<>(controller.runs(), run -> true); runTable = createRunTable(true); runTable.setItems(filteredRuns); VBox.setVgrow(runTable, Priority.ALWAYS); tableCard.getChildren().addAll(header, filters, runTable);
        VBox detail = card(); detailTitle = new Label("Chọn một tiến trình"); detailTitle.getStyleClass().add("section-title"); detailMeta = new Label("Thông tin và bằng chứng sẽ hiển thị tại đây."); detailMeta.getStyleClass().add("muted");
        detailProgress = new ProgressBar(0); detailProgress.setMaxWidth(Double.MAX_VALUE); detailPercent = valueLabel("0%"); HBox progress = new HBox(12, detailProgress, detailPercent); progress.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(detailProgress, Priority.ALWAYS);
        detailStep = new Label("Chưa có bước nào"); detailStep.getStyleClass().add("current-step"); detailError = new Label(""); detailError.setWrapText(true); detailError.getStyleClass().add("error-text");
        StackPane previewFrame = new StackPane(); previewFrame.getStyleClass().add("preview-frame"); previewFrame.setAlignment(Pos.CENTER); previewFrame.setMinHeight(210); previewFrame.setPrefHeight(260);
        Rectangle previewClip = new Rectangle();
        previewClip.widthProperty().bind(previewFrame.widthProperty());
        previewClip.heightProperty().bind(previewFrame.heightProperty());
        previewClip.setArcWidth(18);
        previewClip.setArcHeight(18);
        previewFrame.setClip(previewClip);
        livePreview = new ImageView(); livePreview.setPreserveRatio(true); livePreview.setSmooth(true);
        livePreview.fitWidthProperty().bind(previewFrame.widthProperty().subtract(20));
        livePreview.fitHeightProperty().bind(previewFrame.heightProperty().subtract(20));
        Label placeholder = new Label("XEM TRƯỚC TRỰC TIẾP\nẢnh trình duyệt sẽ cập nhật sau mỗi bước"); placeholder.setTextAlignment(javafx.scene.text.TextAlignment.CENTER); placeholder.getStyleClass().add("preview-placeholder"); livePreview.imageProperty().addListener((o, oldImage, newImage) -> placeholder.setVisible(newImage == null)); previewFrame.getChildren().addAll(placeholder, livePreview);
        stopButton = dangerButton("■  Dừng tiến trình"); stopButton.setTooltip(new Tooltip("Dừng tiến trình đang chạy")); stopButton.setOnAction(e -> stopSelectedRun());
        reportButton = secondaryButton("Xuất kết quả kiểm thử"); reportButton.setTooltip(new Tooltip("Mở tệp Excel kết quả")); reportButton.setOnAction(e -> openArtifact("test-results.xlsx"));
        videoButton = secondaryButton("Mở video kiểm thử"); videoButton.setTooltip(new Tooltip("Mở video thao tác trên trình duyệt")); videoButton.setOnAction(e -> openArtifact("run-video.webm"));
        traceButton = secondaryButton("Mở trace chẩn đoán"); traceButton.setTooltip(new Tooltip("Mở trace để xem lại từng bước và lỗi")); traceButton.setOnAction(e -> openArtifact("trace.zip"));
        folderButton = secondaryButton("Mở thư mục dự án"); folderButton.setTooltip(new Tooltip("Mở thư mục chứa Excel và toàn bộ kết quả")); folderButton.setOnAction(e -> openArtifact(""));
        FlowPane artifactButtons = new FlowPane(8, 8, stopButton, reportButton, videoButton, traceButton, folderButton); artifactButtons.setAlignment(Pos.CENTER_LEFT);
        errorList = new ListView<>(); errorList.setPlaceholder(new Label("Chưa có lỗi nào. Các lỗi của lần chạy sẽ hiển thị ở đây.")); errorList.setPrefHeight(130); errorList.getStyleClass().add("error-list");
        VBox errors = card(); errors.setPadding(new Insets(14)); errors.getChildren().addAll(sectionHeader("Lỗi kiểm thử", "Danh sách lỗi để kiểm tra nhanh"), errorList);
        detail.getChildren().addAll(detailTitle, detailMeta, progress, detailStep, detailError, previewFrame, artifactButtons, errors);
        split.setPrefHeight(760);
        split.getItems().addAll(tableCard, detail); content.getChildren().add(split); VBox.setVgrow(split, Priority.ALWAYS); return scroll(content);
    }

    private Node buildSettings() {
        VBox content = page("settings"); content.getChildren().add(pageHeading("Cấu hình", "Các giá trị có thể chỉnh sửa và lưu vào config/application.properties."));
        VBox editor = card(); editor.getChildren().add(sectionHeader("Thiết lập runner", "Chỉ mật khẩu không được ghi vào file cấu hình")); GridPane form = formGrid();
        configUrlField = new TextField(controller.config().get("env.BASE_URL", "")); configUsernameField = new TextField(controller.config().get("env.USERNAME", "")); configPasswordField = new PasswordField(); configPasswordField.setPromptText("Không ghi vào file cấu hình");
        configHeadlessCheck = new CheckBox("Chạy ẩn trình duyệt theo mặc định"); configHeadlessCheck.setSelected(controller.config().getBoolean("runner.headless", false)); configTimeoutField = new TextField(Integer.toString(controller.config().getInt("runner.defaultTimeoutMs", 15000))); configTemplateField = new TextField(controller.config().get("template.defaultFile", "sample-data/AUTO_TESTING_IMD_BanHang_CayChucNang.xlsx")); configTemplateField.setPromptText("sample-data/Mau_testcase.xlsx");
        form.add(fieldLabel("URL mặc định"), 0, 0); form.add(configUrlField, 1, 0); form.add(fieldLabel("Tài khoản mặc định"), 0, 1); form.add(configUsernameField, 1, 1); form.add(fieldLabel("Mật khẩu phiên này"), 0, 2); form.add(configPasswordField, 1, 2); form.add(fieldLabel("Thời gian chờ (ms)"), 0, 3); form.add(configTimeoutField, 1, 3); form.add(fieldLabel("Tệp mẫu testcase"), 0, 4); form.add(configTemplateField, 1, 4); form.add(new Label(""), 0, 5); form.add(configHeadlessCheck, 1, 5);
        Button save = primaryButton("Lưu cấu hình"); save.setOnAction(e -> saveSettings()); editor.getChildren().addAll(form, save);
        VBox guide = card(); guide.getChildren().addAll(sectionHeader("Cách dùng", "Các khóa thao tác trong Excel vẫn giữ tiếng Anh để runner nhận diện."), bullet("URL, tài khoản, thời gian chờ và đường dẫn tệp mẫu có thể sửa trực tiếp rồi bấm Lưu cấu hình."), bullet("Tệp mẫu nên để đường dẫn tương đối như sample-data/Mau_testcase.xlsx để dễ thay thế, cập nhật và mang sang máy khác."), bullet("Mật khẩu chỉ dùng trong phiên chạy, không ghi vào application.properties."), bullet("Trace là gói chẩn đoán để xem lại DOM, ảnh và từng bước khi lỗi."), bullet("Mỗi dự án có thư mục riêng chứa file Excel và kết quả; xóa dự án sẽ xóa cả thư mục đó."));
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
        if (project == null) return;
        List<TestRun> projectRuns = controller.runs().stream()
                .filter(run -> run.projectId() == project.id()).collect(Collectors.toList());
        TreeItem<OverviewRow> root = buildProjectOverviewTree(project, projectRuns);
        int testCases = root.getChildren().stream().mapToInt(item -> item.getValue().testCaseCount()).sum();
        int passed = root.getChildren().stream().mapToInt(item -> item.getValue().passed()).sum();
        int failed = root.getChildren().stream().mapToInt(item -> item.getValue().failed()).sum();
        long running = projectRuns.stream().filter(run -> run.status() == RunStatus.RUNNING || run.status() == RunStatus.QUEUED).count();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(stage); dialog.setTitle("Tổng quan dự án"); dialog.setHeaderText(project.name());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().add("project-overview-dialog");

        GridPane kpis = new GridPane(); kpis.setHgap(16); kpis.setVgap(10);
        for (int i = 0; i < 4; i++) { ColumnConstraints column = new ColumnConstraints(); column.setPercentWidth(25); kpis.getColumnConstraints().add(column); }
        KpiCard totalCard = projectKpiCard("◆", "Testcase", "accent-blue", Integer.toString(testCases), "trong toàn dự án");
        KpiCard runningCard = projectKpiCard("▶", "Đang chạy", "accent-indigo", Long.toString(running), "tiến trình đang thực thi");
        KpiCard passCard = projectKpiCard("✓", "Đạt", "accent-green", Integer.toString(passed), "lần thực thi testcase");
        KpiCard failCard = projectKpiCard("✕", "Không đạt", "accent-red", Integer.toString(failed), "cần kiểm tra lại");
        kpis.add(totalCard, 0, 0); kpis.add(runningCard, 1, 0); kpis.add(passCard, 2, 0); kpis.add(failCard, 3, 0);

        TreeTableView<OverviewRow> table = createProjectOverviewTable(root);
        VBox tableCard = card(); tableCard.getStyleClass().add("project-overview-table-card");
        tableCard.getChildren().addAll(sectionHeader("Cấu trúc kiểm thử", "Mở rộng từng dòng để xem nhóm chức năng, chức năng con và testcase"), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox content = new VBox(28, kpis, tableCard); content.setPadding(new Insets(4));
        VBox.setMargin(tableCard, new Insets(8, 0, 0, 0));
        dialog.getDialogPane().setContent(content); dialog.getDialogPane().setPrefWidth(1420); dialog.getDialogPane().setPrefHeight(760);
        dialog.showAndWait();
    }

    private KpiCard projectKpiCard(String icon, String title, String accent, String value, String caption) {
        KpiCard card = new KpiCard(icon, title, accent); card.setValue(value); card.setCaption(caption); card.getStyleClass().add("project-kpi-card"); return card;
    }

    private TreeTableView<OverviewRow> createProjectOverviewTable(TreeItem<OverviewRow> root) {
        TreeTableView<OverviewRow> table = new TreeTableView<>(root); table.setShowRoot(false); table.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY); table.setPrefHeight(420);
        Set<OverviewRow> expandedInputs = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<OverviewRow> expandedExpected = Collections.newSetFromMap(new IdentityHashMap<>());
        TreeTableColumn<OverviewRow, String> item = new TreeTableColumn<>("HẠNG MỤC");
        item.setPrefWidth(285); item.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getValue().name()));
        TreeTableColumn<OverviewRow, String> description = new TreeTableColumn<>("MÔ TẢ");
        description.setPrefWidth(205); description.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getValue().description()));
        TreeTableColumn<OverviewRow, String> input = new TreeTableColumn<>("DỮ LIỆU VÀO");
        input.setPrefWidth(165); input.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getValue().input()));
        input.setCellFactory(column -> overviewExpandableTextCell(expandedInputs));
        TreeTableColumn<OverviewRow, String> expected = new TreeTableColumn<>("KẾT QUẢ MONG ĐỢI");
        expected.setPrefWidth(205); expected.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getValue().expected()));
        expected.setCellFactory(column -> overviewExpandableTextCell(expandedExpected));
        TreeTableColumn<OverviewRow, String> testCases = new TreeTableColumn<>("TESTCASE");
        testCases.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getValue().testCaseCount() == 0 ? "—" : Integer.toString(data.getValue().getValue().testCaseCount())));
        testCases.setCellFactory(column -> overviewCenteredCell(""));
        centerOverviewColumn(testCases);
        TreeTableColumn<OverviewRow, String> passed = new TreeTableColumn<>("✓ ĐẠT");
        passed.setCellValueFactory(data -> new ReadOnlyStringWrapper(displayCount(data.getValue().getValue().passed(), "✓")));
        passed.setCellFactory(column -> overviewCenteredCell("overview-pass"));
        centerOverviewColumn(passed);
        TreeTableColumn<OverviewRow, String> failed = new TreeTableColumn<>("✕ KHÔNG ĐẠT");
        failed.setCellValueFactory(data -> new ReadOnlyStringWrapper(displayCount(data.getValue().getValue().failed(), "✕")));
        failed.setCellFactory(column -> overviewCenteredCell("overview-fail"));
        centerOverviewColumn(failed);
        TreeTableColumn<OverviewRow, String> latest = new TreeTableColumn<>("TỔNG HỢP");
        latest.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getValue().latestResult()));
        latest.setCellFactory(column -> overviewCenteredCell("overview-latest"));
        centerOverviewColumn(latest);
        table.getColumns().addAll(item, description, input, expected, testCases, passed, failed, latest);
        table.setRowFactory(view -> new TreeTableRow<OverviewRow>() {
            @Override protected void updateItem(OverviewRow value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeAll("overview-feature-row", "overview-subfeature-row", "overview-testcase-row");
                if (empty || getTreeItem() == null) return;
                int depth = 0; TreeItem<OverviewRow> item = getTreeItem();
                while (item.getParent() != null) { depth++; item = item.getParent(); }
                getStyleClass().add(depth == 1 ? "overview-feature-row" : depth == 2 ? "overview-subfeature-row" : "overview-testcase-row");
            }
        });
        return table;
    }

    private static TreeTableCell<OverviewRow, String> overviewExpandableTextCell(Set<OverviewRow> expandedRows) {
        return new TreeTableCell<OverviewRow, String>() {
            private final Label text = new Label();
            private final Hyperlink toggle = new Hyperlink();
            private final VBox box = new VBox(3, text, toggle);

            {
                text.setWrapText(false);
                text.setTextOverrun(OverrunStyle.ELLIPSIS);
                text.maxWidthProperty().bind(widthProperty().subtract(12));
                toggle.getStyleClass().add("overview-more-link");
                toggle.setOnAction(event -> {
                    OverviewRow row = getTreeTableRow() == null ? null : getTreeTableRow().getItem();
                    if (row == null) return;
                    if (expandedRows.contains(row)) expandedRows.remove(row);
                    else expandedRows.add(row);
                    getTreeTableView().refresh();
                });
            }

            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeAll("overview-text-cell");
                if (empty || value == null || value.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                OverviewRow row = getTreeTableRow() == null ? null : getTreeTableRow().getItem();
                boolean longText = value.length() > 55;
                boolean expanded = row != null && expandedRows.contains(row);
                text.setWrapText(expanded);
                text.setMaxHeight(expanded ? Double.MAX_VALUE : 18);
                text.setPrefHeight(expanded ? Region.USE_COMPUTED_SIZE : 18);
                text.setText(longText && !expanded ? value.substring(0, 55).trim() + "..." : value);
                toggle.setVisible(longText);
                toggle.setManaged(longText);
                toggle.setText(expanded ? "Thu gọn" : "Xem thêm");
                getStyleClass().add("overview-text-cell");
                setText(null);
                setGraphic(box);
            }
        };
    }

    private static TreeTableCell<OverviewRow, String> overviewCenteredCell(String styleClass) {
        return new TreeTableCell<OverviewRow, String>() {
            {
                setAlignment(Pos.CENTER);
                setStyle("-fx-alignment: CENTER;");
            }

            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
                setAlignment(Pos.CENTER);
                setStyle("-fx-alignment: CENTER;");
                getStyleClass().removeAll("overview-pass", "overview-fail", "overview-latest", "overview-center");
                if (!empty) {
                    getStyleClass().add("overview-center");
                    if (!styleClass.isBlank()) getStyleClass().add(styleClass);
                }
            }
        };
    }

    private static void centerOverviewColumn(TreeTableColumn<OverviewRow, String> column) {
        column.getStyleClass().add("overview-center-column");
        column.setStyle("-fx-alignment: CENTER;");
    }

    private static String displayCount(int value, String icon) { return value == 0 ? "—" : icon + " " + value; }

    private TreeItem<OverviewRow> buildProjectOverviewTree(TestProject project, List<TestRun> runs) {
        Map<Long, FeatureSummary> summaries = new LinkedHashMap<>();
        for (TestFeature feature : controller.features(project.id())) summaries.put(feature.id(), new FeatureSummary(feature));
        for (TestRun run : runs) {
            FeatureSummary summary = summaries.get(run.featureId()); if (summary == null) continue;
            List<CaseOutcome> outcomes = readCaseOutcomes(run);
            if (outcomes.isEmpty()) outcomes = List.of(CaseOutcome.fromRun(run));
            outcomes.forEach(summary::add);
        }
        TreeItem<OverviewRow> root = new TreeItem<>(new OverviewRow("Dự án", "", "", "", 0, 0, 0, "")); root.setExpanded(true);
        summaries.values().forEach(summary -> root.getChildren().add(summary.toTreeItem())); return root;
    }

    private List<CaseOutcome> readCaseOutcomes(TestRun run) {
        Path report = run.artifactDirectory().resolve("test-results.xlsx"); if (!Files.isRegularFile(report)) return Collections.emptyList();
        try (InputStream input = Files.newInputStream(report); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheet(run.featureName());
            if (sheet == null) sheet = workbook.getSheet("Kết quả kiểm thử");
            if (sheet == null && workbook.getNumberOfSheets() > 1) sheet = workbook.getSheetAt(1);
            if (sheet == null) return Collections.emptyList();
            Row header = sheet.getRow(0); if (header == null) return Collections.emptyList();
            Map<String, Integer> columns = overviewHeaderMap(header); DataFormatter formatter = new DataFormatter();
            Map<String, CaseOutcome> outcomes = new LinkedHashMap<>();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index); if (row == null) continue;
                String testCase = overviewCell(row, columns, "testcase", formatter); if (testCase.isBlank()) continue;
                String subFeature = overviewCell(row, columns, "subfeature", formatter); if (subFeature.isBlank()) subFeature = "Chưa phân loại"; final String sectionName = subFeature;
                String status = overviewCell(row, columns, "status", formatter); boolean passed = normalizeOverview(status).equals("dat") || normalizeOverview(status).equals("pass");
                String description = overviewCell(row, columns, "description", formatter); String inputValue = overviewCell(row, columns, "input", formatter); String expected = overviewCell(row, columns, "expected", formatter);
                String key = sectionName + "\u0000" + testCase; CaseOutcome outcome = outcomes.computeIfAbsent(key, ignored -> new CaseOutcome(sectionName, testCase, true)); outcome.record(passed, description, inputValue, expected);
            }
            return new ArrayList<>(outcomes.values());
        } catch (Exception ignored) { return Collections.emptyList(); }
    }

    private static Map<String, Integer> overviewHeaderMap(Row row) {
        Map<String, Integer> result = new HashMap<>(); DataFormatter formatter = new DataFormatter();
        for (org.apache.poi.ss.usermodel.Cell cell : row) {
            String value = normalizeOverview(formatter.formatCellValue(cell));
            if (Set.of("chucnangcon", "chucnangchitiet", "subfeature").contains(value)) result.put("subfeature", cell.getColumnIndex());
            else if (Set.of("matestcase", "testcaseid", "testcase").contains(value)) result.put("testcase", cell.getColumnIndex());
            else if (Set.of("mota", "description").contains(value)) result.put("description", cell.getColumnIndex());
            else if (Set.of("dulieuvao", "input").contains(value)) result.put("input", cell.getColumnIndex());
            else if (Set.of("ketquamongdoi", "expected", "output").contains(value)) result.put("expected", cell.getColumnIndex());
            else if (Set.of("trangthai", "status", "ketqua", "result").contains(value)) result.put("status", cell.getColumnIndex());
        }
        return result;
    }

    private static String overviewCell(Row row, Map<String, Integer> columns, String name, DataFormatter formatter) {
        Integer index = columns.get(name); if (index == null || row.getCell(index) == null) return ""; return formatter.formatCellValue(row.getCell(index)).trim();
    }

    private static String normalizeOverview(String value) {
        return java.text.Normalizer.normalize(value == null ? "" : value, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private void chooseExcelFile() { FileChooser chooser = new FileChooser(); chooser.setTitle("Chọn tệp testcase Excel"); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tệp Excel", "*.xlsx")); java.io.File file = chooser.showOpenDialog(stage); if (file != null) selectExcelFile(file.toPath()); }

    private void selectExcelFile(Path file) { selectedExcelFile = file; excelFileLabel.setText(file.getFileName().toString()); setValidation("Chưa kiểm tra tệp mới", false, false); }
    private static boolean isExcelFile(Path file) { return file != null && Files.isRegularFile(file) && file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xlsx"); }

    private void downloadTestCaseTemplate() {
        Path template = controller.config().defaultTestCaseTemplate();
        if (!Files.isRegularFile(template)) { showMessage(Alert.AlertType.WARNING, "Không tìm thấy mẫu testcase", "Kiểm tra khóa template.defaultFile trong config/application.properties."); return; }
        FileChooser chooser = new FileChooser(); chooser.setTitle("Lưu mẫu testcase"); chooser.setInitialFileName(template.getFileName().toString()); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tệp Excel", "*.xlsx"));
        java.io.File target = chooser.showSaveDialog(stage); if (target == null) return;
        try { Files.copy(template, target.toPath(), StandardCopyOption.REPLACE_EXISTING); showMessage(Alert.AlertType.INFORMATION, "Đã tải mẫu testcase", "Mẫu Excel đã được lưu tại:\n" + target.getAbsolutePath()); }
        catch (IOException error) { showMessage(Alert.AlertType.ERROR, "Không lưu được mẫu testcase", "Hãy kiểm tra quyền ghi tệp rồi thử lại."); }
    }

    private void validateExcel() { if (selectedExcelFile == null) { showMessage(Alert.AlertType.WARNING, "Chưa có tệp", "Hãy chọn tệp Excel trước."); return; } try { ImportResult result = controller.validateExcel(selectedExcelFile, selectedFeature == null ? null : selectedFeature.name(), excelStartRow(), excelStartColumn()); String warning = result.warnings().isEmpty() ? "" : " · " + result.warnings().get(0); setValidation("Hợp lệ · " + result.testCaseCount() + " testcase · " + result.steps().size() + " bước" + warning, true, false); } catch (RuntimeException error) { setValidation("Không hợp lệ · " + error.getMessage(), false, true); } }

    private void startRun() { try { if (selectedProject == null) throw new IllegalArgumentException("Hãy chọn dự án"); if (selectedFeature == null) throw new IllegalArgumentException("Hãy chọn chức năng"); if (selectedExcelFile == null) throw new IllegalArgumentException("Hãy chọn tệp Excel"); int startRow = excelStartRow(); int startColumn = excelStartColumn(); controller.validateExcel(selectedExcelFile, selectedFeature.name(), startRow, startColumn); TestRun run = controller.startRun(selectedProject, selectedFeature, selectedExcelFile, headlessCheck.isSelected(), usernameField.getText(), passwordField.getText(), startRow, startColumn); passwordField.clear(); showPage("runs"); runTable.getSelectionModel().select(run); showRunDetail(run); } catch (RuntimeException error) { showError(error); } }

    private void stopSelectedRun() { TestRun run = runTable.getSelectionModel().getSelectedItem(); if (run == null) return; if (!controller.cancel(run.id())) showMessage(Alert.AlertType.INFORMATION, "Không có tiến trình đang chạy", "Tiến trình này đã kết thúc."); }

    private void showRunDetail(TestRun run) { if (run == null || detailTitle == null) return; detailTitle.setText(run.projectName() + "  /  " + run.featureName()); detailMeta.setText(DATE_TIME.format(run.startedAt()) + "  ·  " + Path.of(run.sourceFile()).getFileName() + "  ·  " + statusText(run.status())); detailProgress.setProgress(run.progress() / 100.0); detailPercent.setText(run.progress() + "%"); detailStep.setText(run.currentStep() == null || run.currentStep().isBlank() ? "Chưa có bước nào" : run.currentStep()); detailError.setText(run.errorMessage() == null ? "" : run.errorMessage()); stopButton.setDisable(run.status() != RunStatus.RUNNING && run.status() != RunStatus.QUEUED); reportButton.setDisable(!Files.exists(run.artifactDirectory().resolve("test-results.xlsx"))); videoButton.setDisable(!Files.exists(run.artifactDirectory().resolve("run-video.webm"))); traceButton.setDisable(!Files.exists(run.artifactDirectory().resolve("trace.zip"))); folderButton.setDisable(!Files.exists(run.artifactDirectory())); Path preview = previews.get(run.id()); if (preview == null) preview = latestScreenshot(run.artifactDirectory().resolve("screenshots")); if (preview != null) setPreview(preview); else livePreview.setImage(null); }

    private static String statusText(RunStatus status) { switch (status) { case QUEUED: return "Đang chờ"; case RUNNING: return "Đang chạy"; case PASSED: return "Đạt"; case FAILED: return "Không đạt"; case CANCELLED: return "Đã dừng"; default: return status.name(); } }
    private void saveSettings() { try { int timeout = Integer.parseInt(configTimeoutField.getText().trim()); if (timeout < 100) throw new NumberFormatException(); if (configTemplateField.getText().trim().isBlank()) throw new IllegalArgumentException("Hãy nhập đường dẫn tệp mẫu testcase"); controller.config().set("env.BASE_URL", configUrlField.getText().trim()); controller.config().set("env.USERNAME", configUsernameField.getText().trim()); controller.config().set("template.defaultFile", configTemplateField.getText().trim()); controller.config().set("runner.headless", Boolean.toString(configHeadlessCheck.isSelected())); controller.config().set("runner.defaultTimeoutMs", Integer.toString(timeout)); controller.config().save(); usernameField.setText(configUsernameField.getText()); passwordField.setText(configPasswordField.getText()); configPasswordField.clear(); headlessCheck.setSelected(configHeadlessCheck.isSelected()); showMessage(Alert.AlertType.INFORMATION, "Đã lưu cấu hình", "Cấu hình sẽ được áp dụng cho các lần chạy tiếp theo. Mật khẩu chỉ được áp dụng cho phiên này."); } catch (NumberFormatException error) { showMessage(Alert.AlertType.ERROR, "Thời gian chờ không hợp lệ", "Hãy nhập số mili-giây lớn hơn hoặc bằng 100."); } catch (RuntimeException error) { showError(error); } }

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
    private static Button treeRowButton(String text) { Button button = new Button(text); button.getStyleClass().add("tree-row-button"); button.setFocusTraversable(false); return button; }
    private static Label fieldLabel(String text) { Label label = new Label(text); label.getStyleClass().add("field-label"); return label; }
    private static Label valueLabel(String text) { Label label = new Label(text); label.getStyleClass().add("value-label"); return label; }
    private static GridPane formGrid() { GridPane grid = new GridPane(); grid.setHgap(16); grid.setVgap(13); ColumnConstraints labels = new ColumnConstraints(150); ColumnConstraints inputs = new ColumnConstraints(); inputs.setHgrow(Priority.ALWAYS); inputs.setFillWidth(true); grid.getColumnConstraints().addAll(labels, inputs); return grid; }
    private static Separator separator() { Separator separator = new Separator(); separator.getStyleClass().add("soft-separator"); return separator; }
    private static Dialog<ButtonType> formDialog(String title, String subtitle) { Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle(title); dialog.setHeaderText(subtitle); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL); dialog.getDialogPane().setPrefWidth(560); return dialog; }
    private static Label bullet(String text) { Label label = new Label("✓  " + text); label.getStyleClass().add("settings-bullet"); label.setWrapText(true); return label; }
    private static FlowPane wrapChips(String... values) { FlowPane pane = new FlowPane(8, 8); for (String value : values) { Label chip = new Label(value); chip.getStyleClass().add("action-chip"); pane.getChildren().add(chip); } return pane; }
    private static ColumnConstraints cloneColumn(ColumnConstraints source) { ColumnConstraints copy = new ColumnConstraints(); copy.setPercentWidth(source.getPercentWidth()); return copy; }
    private void setValidation(String text, boolean success, boolean error) { validationLabel.setText(text); validationLabel.getStyleClass().removeAll("validation-neutral", "validation-success", "validation-error"); validationLabel.getStyleClass().add(success ? "validation-success" : error ? "validation-error" : "validation-neutral"); }
    private int excelStartRow() { return parsePositiveInt(excelStartRowField == null ? "1" : excelStartRowField.getText(), "Dòng header"); }
    private int excelStartColumn() { return parseExcelColumn(excelStartColumnField == null ? "A" : excelStartColumnField.getText()); }
    private static int parsePositiveInt(String value, String label) { try { int parsed = Integer.parseInt(value == null ? "" : value.trim()); if (parsed < 1) throw new NumberFormatException(); return parsed; } catch (NumberFormatException error) { throw new IllegalArgumentException(label + " phải là số lớn hơn hoặc bằng 1"); } }
    private static int parseExcelColumn(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isBlank()) throw new IllegalArgumentException("Cột bắt đầu không được để trống");
        if (text.matches("\\d+")) return parsePositiveInt(text, "Cột bắt đầu");
        int result = 0;
        for (char ch : text.toUpperCase(Locale.ROOT).toCharArray()) {
            if (ch < 'A' || ch > 'Z') throw new IllegalArgumentException("Cột bắt đầu phải là số hoặc chữ cột Excel như A, B, AA");
            result = result * 26 + (ch - 'A' + 1);
        }
        return result;
    }
    private void refreshSelectionLabels() { selectedProjectLabel.setText(selectedProject == null ? "Chưa chọn dự án" : selectedProject.name()); selectedFeatureLabel.setText(selectedFeature == null ? "Chưa chọn chức năng" : selectedFeature.name()); }
    private void selectTreeValue(Object value) { for (TreeItem<Object> projectItem : projectTree.getRoot().getChildren()) { if (sameEntity(projectItem.getValue(), value)) { projectTree.getSelectionModel().select(projectItem); return; } for (TreeItem<Object> featureItem : projectItem.getChildren()) if (sameEntity(featureItem.getValue(), value)) { projectTree.getSelectionModel().select(featureItem); return; } } }
    private static boolean sameEntity(Object left, Object right) { if (left instanceof TestProject && right instanceof TestProject) return ((TestProject) left).id() == ((TestProject) right).id(); if (left instanceof TestFeature && right instanceof TestFeature) return ((TestFeature) left).id() == ((TestFeature) right).id(); return Objects.equals(left, right); }
    private boolean isSelected(TestRun run) { return runTable != null && runTable.getSelectionModel().getSelectedItem() != null && runTable.getSelectionModel().getSelectedItem().id().equals(run.id()); }
    private void showError(Throwable error) { Throwable current = error; while (current.getCause() != null) current = current.getCause(); showMessage(Alert.AlertType.ERROR, "Không thể thực hiện", "Chi tiết lỗi: " + (current.getMessage() == null ? "Lỗi không xác định" : current.getMessage())); }
    private void showMessage(Alert.AlertType type, String title, String message) { Alert alert = new Alert(type); alert.initOwner(stage); alert.setTitle("AUTO TESTING IMD"); alert.setHeaderText(title); alert.setContentText(message == null ? "Lỗi không xác định" : message); alert.showAndWait(); }

    private static final class OverviewRow {
        private final String name;
        private final String description;
        private final String input;
        private final String expected;
        private final int testCaseCount;
        private final int passed;
        private final int failed;
        private final String latestResult;

        private OverviewRow(String name, String description, String input, String expected, int testCaseCount, int passed, int failed, String latestResult) {
            this.name = name;
            this.description = description;
            this.input = input;
            this.expected = expected;
            this.testCaseCount = testCaseCount;
            this.passed = passed;
            this.failed = failed;
            this.latestResult = latestResult;
        }

        String name() { return name; }
        String description() { return description; }
        String input() { return input; }
        String expected() { return expected; }
        int testCaseCount() { return testCaseCount; }
        int passed() { return passed; }
        int failed() { return failed; }
        String latestResult() { return latestResult; }
    }

    private static final class CaseOutcome {
        private final String subFeature;
        private final String testCase;
        private String description = "";
        private String input = "";
        private String expected = "";
        private int passed;
        private int failed;

        private CaseOutcome(String subFeature, String testCase, boolean pending) {
            this.subFeature = subFeature;
            this.testCase = testCase;
        }

        static CaseOutcome fromRun(TestRun run) {
            CaseOutcome result = new CaseOutcome("Chưa có chi tiết", "Lần chạy " + run.id(), false);
            if (run.status() == RunStatus.PASSED) result.passed = 1;
            else if (run.status() == RunStatus.FAILED || run.status() == RunStatus.CANCELLED) result.failed = 1;
            return result;
        }

        void record(boolean isPassed, String description, String input, String expected) {
            if (this.description.isBlank() && description != null) this.description = description;
            if (this.input.isBlank() && input != null) this.input = input;
            if (this.expected.isBlank() && expected != null) this.expected = expected;
            if (failed > 0) return;
            if (isPassed) passed = 1;
            else { passed = 0; failed = 1; }
        }
        boolean latestPassed() { return failed == 0 && passed > 0; }
    }

    private static final class FeatureSummary {
        private final TestFeature feature;
        private final Map<String, Map<String, CaseOutcome>> sections = new LinkedHashMap<>();

        private FeatureSummary(TestFeature feature) { this.feature = feature; }

        void add(CaseOutcome outcome) {
            Map<String, CaseOutcome> cases = sections.computeIfAbsent(outcome.subFeature, ignored -> new LinkedHashMap<>());
            CaseOutcome existing = cases.get(outcome.testCase);
            if (existing == null) { cases.put(outcome.testCase, outcome); return; }
            existing.passed += outcome.passed; existing.failed += outcome.failed;
        }

        TreeItem<OverviewRow> toTreeItem() {
            int passed = passed(); int failed = failed(); int testCases = testCaseCount();
            TreeItem<OverviewRow> featureItem = new TreeItem<>(new OverviewRow(feature.name(), "", "", "", testCases, passed, failed, latest(passed, failed)));
            featureItem.setExpanded(true);
            sections.forEach((section, cases) -> {
                int sectionPassed = cases.values().stream().mapToInt(item -> item.passed).sum();
                int sectionFailed = cases.values().stream().mapToInt(item -> item.failed).sum();
                TreeItem<OverviewRow> sectionItem = new TreeItem<>(new OverviewRow(section, "", "", "", cases.size(), sectionPassed, sectionFailed, latest(sectionPassed, sectionFailed)));
                sectionItem.setExpanded(true);
                cases.values().forEach(testCase -> sectionItem.getChildren().add(new TreeItem<>(new OverviewRow(testCase.testCase, testCase.description, testCase.input, testCase.expected, 1, testCase.passed, testCase.failed, latest(testCase.passed, testCase.failed)))));
                featureItem.getChildren().add(sectionItem);
            });
            return featureItem;
        }

        int testCaseCount() { return sections.values().stream().mapToInt(Map::size).sum(); }
        int passed() { return sections.values().stream().flatMap(cases -> cases.values().stream()).mapToInt(item -> item.passed).sum(); }
        int failed() { return sections.values().stream().flatMap(cases -> cases.values().stream()).mapToInt(item -> item.failed).sum(); }
        private static String latest(int passed, int failed) { return failed > 0 ? "✕ Không đạt" : passed > 0 ? "✓ Đạt" : "Chưa chạy"; }
    }
}

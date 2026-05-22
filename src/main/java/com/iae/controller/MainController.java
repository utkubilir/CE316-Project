package com.iae.controller;

import com.iae.model.Configuration;
import com.iae.model.StudentResult;
import com.iae.model.SubmissionInfo;
import com.iae.model.TestStatus;
import com.iae.service.ConfigurationService;
import com.iae.service.FileManager;
import com.iae.service.ProjectService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainController {

    private static final String FILTER_ALL = "All Results";
    private static final String FILTER_PASSED = "Passed";
    private static final String FILTER_FAILED = "Failed";
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
    private static final List<String> STATUS_CELL_CLASSES = List.of(
            "status-pill",
            "status-passed",
            "status-failed",
            "status-compile",
            "status-runtime",
            "status-timeout",
            "status-mismatch",
            "status-missing",
            "status-pending"
    );

    @FXML private ComboBox<String> configCombo;
    @FXML private Label expectedOutputHintLabel;
    @FXML private TextField sourceFileField;
    @FXML private TextField compileCmdField;
    @FXML private TextField runCmdField;
    @FXML private TextField expectedOutputField;
    @FXML private TextField submissionFolderField;
    @FXML private TextArea expectedOutputTextArea;
    @FXML private Button runTestsBtn;
    @FXML private Button cancelRunBtn;
    @FXML private Button newProjectBtn;
    @FXML private Button openProjectBtn;
    @FXML private Button saveProjectBtn;
    @FXML private Button deleteProjectBtn;
    @FXML private Button browseSubmissionsBtn;
    @FXML private Button browseExpectedOutputBtn;
    @FXML private Button saveConfigBtn;
    @FXML private ProgressBar runProgress;

    @FXML private Label projectNameLabel;
    @FXML private Label configNameLabel;
    @FXML private Label submissionCountLabel;
    @FXML private Label lastRunLabel;
    @FXML private Label projectValidationLabel;
    @FXML private Label configValidationLabel;
    @FXML private Label configLanguageLabel;
    @FXML private Label submissionTableHint;
    @FXML private Label resultsSummaryLabel;
    @FXML private Label currentRunLabel;

    @FXML private TableView<StudentResult> resultsTable;
    @FXML private TableColumn<StudentResult, String> colStudentId;
    @FXML private TableColumn<StudentResult, TestStatus> colStatus;
    @FXML private TableColumn<StudentResult, String> colDetails;
    @FXML private TableColumn<StudentResult, Void> colActions;
    @FXML private ComboBox<String> statusFilterCombo;

    @FXML private TableView<SubmissionInfo> submissionsTable;
    @FXML private TableColumn<SubmissionInfo, String> colSubZip;
    @FXML private TableColumn<SubmissionInfo, String> colSubStudent;
    @FXML private TableColumn<SubmissionInfo, String> colSubSize;

    @FXML private Label statusLeft;
    @FXML private Label statusRight;

    private final ObservableList<StudentResult> resultsData = FXCollections.observableArrayList();
    private final FilteredList<StudentResult> filteredResultsData = new FilteredList<>(resultsData, result -> true);
    private final ObservableList<SubmissionInfo> submissionsData = FXCollections.observableArrayList();
    private final ProjectService projectService = new ProjectService();
    private final ConfigurationService configurationService = new ConfigurationService();
    private final FileManager fileManager = new FileManager();
    private Task<List<StudentResult>> evaluationTask;

    private boolean suppressConfigurationListener = false;
    private boolean suppressFormListeners = false;
    private boolean formInitialized = false;
    private boolean showValidationMessages = false;
    private boolean projectDirty = false;
    private boolean configDirty = false;
    private String activeConfigurationName = "";
    private String activeConfigurationLanguage = "";

    @FXML
    public void initialize() {
        configurationService.seedDefaultsIfEmpty();

        configureResultTable();
        configureSubmissionsTable();
        configureStatusFilter();
        configureFieldListeners();
        configureTooltips();
        configureAccessibility();
        configureResponsiveTables();

        suppressFormListeners = true;
        refreshConfigurationChoices(null);
        configCombo.setOnAction(event -> {
            if (!suppressConfigurationListener) {
                loadSelectedConfiguration();
            }
            updateContextLabels();
            validateForm(false);
        });

        submissionFolderField.setText("");
        refreshSubmissionsTable();
        suppressFormListeners = false;
        formInitialized = true;

        statusLeft.setText("Ready. Create or open a project, then select submissions and expected output.");
        statusRight.setText("");
        updateResultsSummary();
        updateContextLabels();
        validateForm(false);
    }

    private void configureResultTable() {
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(TestStatus item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll(STATUS_CELL_CLASSES);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    getStyleClass().addAll("status-pill", statusStyleClass(item));
                    setText(item.display());
                    setTooltip(new Tooltip(item.name()));
                }
            }
        });

        colDetails.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                StudentResult result = getTableRow() != null ? getTableRow().getItem() : null;
                String text = result != null ? detailsText(result) : cleanText(item);
                setText(text);
                setTooltip(text.isBlank() ? null : new Tooltip(text));
            }
        });

        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button button = new Button("Details");

            {
                button.getStyleClass().add("details-btn");
                button.setOnAction(event -> {
                    StudentResult result = getTableRow() != null ? getTableRow().getItem() : null;
                    if (result != null) {
                        showResultDetailsDialog(result);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
            }
        });

        resultsTable.setItems(filteredResultsData);
        resultsTable.setPlaceholder(new Label("Run tests to see per-student results."));
        resultsTable.setRowFactory(tv -> {
            TableRow<StudentResult> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showResultDetailsDialog(row.getItem());
                }
            });
            return row;
        });
    }

    private void configureSubmissionsTable() {
        colSubZip.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        colSubStudent.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colSubSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        submissionsTable.setItems(submissionsData);
        submissionsTable.setPlaceholder(new Label("Select a submissions folder to preview ZIP files."));
    }

    private void configureStatusFilter() {
        statusFilterCombo.setItems(FXCollections.observableArrayList(
                FILTER_ALL,
                FILTER_PASSED,
                FILTER_FAILED,
                TestStatus.COMPILATION_ERROR.display(),
                TestStatus.RUNTIME_ERROR.display(),
                TestStatus.TIMEOUT.display(),
                TestStatus.OUTPUT_MISMATCH.display(),
                TestStatus.MISSING_SOURCE.display(),
                TestStatus.EXTRACTION_ERROR.display()
        ));
        statusFilterCombo.getSelectionModel().select(FILTER_ALL);
        statusFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            applyResultsFilter();
            updateResultsSummary();
            updateResultsPlaceholder();
        });
    }

    private void configureFieldListeners() {
        submissionFolderField.textProperty().addListener((obs, oldVal, newVal) -> {
            markFormEdited();
            refreshSubmissionsTable();
        });
        sourceFileField.textProperty().addListener((obs, oldVal, newVal) -> {
            markConfigurationEdited();
            validateForm();
        });
        compileCmdField.textProperty().addListener((obs, oldVal, newVal) -> {
            markConfigurationEdited();
            validateForm();
        });
        runCmdField.textProperty().addListener((obs, oldVal, newVal) -> {
            markConfigurationEdited();
            validateForm();
        });
        expectedOutputField.textProperty().addListener((obs, oldVal, newVal) -> {
            markProjectDirty();
            validateForm();
            loadExpectedOutputContent();
        });
        resultsData.addListener((javafx.collections.ListChangeListener<StudentResult>) change -> {
            updateResultsSummary();
            updateContextLabels();
            updateResultsPlaceholder();
        });
        filteredResultsData.addListener((javafx.collections.ListChangeListener<StudentResult>) change -> {
            updateResultsSummary();
            updateResultsPlaceholder();
        });
    }

    private void configureTooltips() {
        runTestsBtn.setTooltip(new Tooltip("Complete the project inputs before running tests."));
        cancelRunBtn.setTooltip(new Tooltip("Stop after the current submission finishes."));
        configCombo.setTooltip(new Tooltip("Saved configuration used for compile and run commands."));
        submissionFolderField.setTooltip(new Tooltip("Folder containing student ZIP files."));
        expectedOutputField.setTooltip(new Tooltip("Text file used as the expected program output."));
    }

    private void configureAccessibility() {
        newProjectBtn.setMnemonicParsing(true);
        openProjectBtn.setMnemonicParsing(true);
        saveProjectBtn.setMnemonicParsing(true);
        deleteProjectBtn.setMnemonicParsing(true);
        browseSubmissionsBtn.setMnemonicParsing(true);
        browseExpectedOutputBtn.setMnemonicParsing(true);
        saveConfigBtn.setMnemonicParsing(true);
        runTestsBtn.setMnemonicParsing(true);
        cancelRunBtn.setMnemonicParsing(true);

        configCombo.setAccessibleText("Active configuration");
        configLanguageLabel.setAccessibleText("Selected configuration language");
        submissionFolderField.setAccessibleText("Submission folder path");
        sourceFileField.setAccessibleText("Source file name");
        compileCmdField.setAccessibleText("Compile command");
        runCmdField.setAccessibleText("Run command");
        expectedOutputField.setAccessibleText("Expected output file path");
        statusFilterCombo.setAccessibleText("Results status filter");
        resultsTable.setAccessibleText("Student test results");
        submissionsTable.setAccessibleText("Discovered student submission ZIP files");
    }

    private void configureResponsiveTables() {
        resultsTable.widthProperty().addListener((obs, oldWidth, newWidth) -> resizeResultColumns());
        submissionsTable.widthProperty().addListener((obs, oldWidth, newWidth) -> resizeSubmissionColumns());
        Platform.runLater(() -> {
            resizeResultColumns();
            resizeSubmissionColumns();
        });
    }

    private void resizeResultColumns() {
        double width = Math.max(520, resultsTable.getWidth() - 18);
        colStudentId.setPrefWidth(Math.max(110, width * 0.18));
        colStatus.setPrefWidth(Math.max(130, width * 0.20));
        colActions.setPrefWidth(88);
        colDetails.setPrefWidth(Math.max(220, width - colStudentId.getPrefWidth()
                - colStatus.getPrefWidth() - colActions.getPrefWidth()));
    }

    private void resizeSubmissionColumns() {
        double width = Math.max(420, submissionsTable.getWidth() - 18);
        colSubSize.setPrefWidth(Math.max(90, width * 0.18));
        colSubStudent.setPrefWidth(Math.max(120, width * 0.28));
        colSubZip.setPrefWidth(Math.max(190, width - colSubStudent.getPrefWidth() - colSubSize.getPrefWidth()));
    }

    @FXML
    private void onNewProject() {
        if (isRunning()) {
            return;
        }

        javafx.scene.control.TextInputDialog dialog =
                new javafx.scene.control.TextInputDialog("New Project");
        dialog.setTitle("New Project");
        dialog.setHeaderText("Create a New Project");
        dialog.setContentText("Please enter project name:");
        styleDialog(dialog);

        Optional<String> nameOpt = dialog.showAndWait();
        if (nameOpt.isEmpty() || nameOpt.get().trim().isEmpty()) {
            return;
        }
        String name = nameOpt.get().trim();

        com.iae.model.Project p = new com.iae.model.Project();
        p.setName(name);

        Configuration chosen = promptForProjectConfiguration();
        if (chosen != null) {
            applyConfigurationToForm(chosen);
            p.setConfiguration(chosen);
        }

        projectService.setCurrentProject(p);
        submissionFolderField.setText("");
        resultsData.clear();
        projectDirty = true;
        showValidationMessages = false;
        statusLeft.setText("New project created: " + name);
        lastRunLabel.setText("Not run yet");
        updateContextLabels();
        validateForm(false);
    }

    private Configuration promptForProjectConfiguration() {
        List<String> savedNames = configurationService.listConfigurationNames();

        final String createNew = "[ Create New Configuration ]";
        List<String> choices = new ArrayList<>();
        choices.add(createNew);
        choices.addAll(savedNames);

        ChoiceDialog<String> picker = new ChoiceDialog<>(choices.get(0), choices);
        picker.setTitle("Project Configuration");
        picker.setHeaderText("Use existing configuration or create a new one?");
        picker.setContentText("Configuration:");
        styleDialog(picker);

        Optional<String> result = picker.showAndWait();
        if (result.isEmpty()) {
            return null;
        }

        if (createNew.equals(result.get())) {
            return showCreateConfigurationDialog();
        }

        return configurationService.getConfiguration(result.get());
    }

    private void applyConfigurationToForm(Configuration c) {
        if (c == null) {
            return;
        }
        suppressConfigurationListener = true;
        suppressFormListeners = true;
        try {
            activeConfigurationName = cleanText(c.getName());
            activeConfigurationLanguage = cleanText(c.getLanguage());
            if (!activeConfigurationName.isBlank() && !configCombo.getItems().contains(activeConfigurationName)) {
                configCombo.getItems().add(activeConfigurationName);
            }
            configCombo.setValue(activeConfigurationName.isBlank() ? null : activeConfigurationName);
            configLanguageLabel.setText(activeConfigurationLanguage.isBlank() ? "No language specified" : activeConfigurationLanguage);
            sourceFileField.setText(c.getSourceFileName() != null ? c.getSourceFileName() : "");
            compileCmdField.setText(c.getCompileCommand() != null ? c.getCompileCommand() : "");
            runCmdField.setText(c.getRunCommand() != null ? c.getRunCommand() : "");
        } finally {
            suppressFormListeners = false;
            suppressConfigurationListener = false;
        }
        configDirty = false;
        updateContextLabels();
        validateForm(false);
    }

    private Configuration readConfigurationFromForm(String name) {
        Configuration cfg = new Configuration(name, activeConfigurationLanguage);
        cfg.setSourceFileName(cleanText(sourceFileField.getText()));
        cfg.setCompileCommand(cleanText(compileCmdField.getText()));
        cfg.setRunCommand(cleanText(runCmdField.getText()));
        cfg.setCompiled(cfg.getCompileCommand() != null && !cfg.getCompileCommand().isBlank());
        return cfg;
    }

    @FXML
    private void onOpenProject() {
        if (isRunning()) {
            return;
        }

        List<String> names = projectService.getAllProjectNames();
        if (names.isEmpty()) {
            info("Open Project", "No saved projects found.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(names.get(0), names);
        dialog.setTitle("Open Project");
        dialog.setHeaderText("Select a project to open");
        dialog.setContentText("Project:");
        styleDialog(dialog);

        dialog.showAndWait().ifPresent(name -> {
            com.iae.model.Project p = projectService.loadProject(name);
            if (p != null) {
                projectService.setCurrentProject(p);
                submissionFolderField.setText(p.getSubmissionFolder() != null ? p.getSubmissionFolder() : "");
                expectedOutputField.setText(p.getExpectedOutputPath() != null ? p.getExpectedOutputPath() : "");
                if (p.getConfiguration() != null) {
                    applyConfigurationToForm(p.getConfiguration());
                }
                resultsData.clear();
                if (p.getResults() != null) {
                    resultsData.addAll(p.getResults());
                }
                projectDirty = false;
                showValidationMessages = false;
                statusLeft.setText("Project loaded: " + name);
                lastRunLabel.setText(resultsData.isEmpty() ? "Not run yet" : "Loaded saved results");
                updateContextLabels();
                validateForm(false);
            } else {
                info("Error", "Could not load project.");
            }
        });
    }

    @FXML
    private void onSaveProject() {
        if (isRunning()) {
            return;
        }

        com.iae.model.Project p = projectService.getCurrentProject();
        if (p == null) {
            info("Save Project", "No active project to save. Please create or open a project first.");
            return;
        }

        p.setSubmissionFolder(cleanText(submissionFolderField.getText()));
        p.setExpectedOutputPath(cleanText(expectedOutputField.getText()));
        p.setConfiguration(readConfigurationFromForm(configurationNameForSave(p.getName() + " Config")));
        p.setResults(new ArrayList<>(resultsData));

        try {
            projectService.saveProject(p);
            projectDirty = false;
            statusLeft.setText("Project saved successfully.");
            updateContextLabels();
        } catch (Exception e) {
            info("Error", "Failed to save project: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteProject() {
        if (isRunning()) {
            return;
        }

        List<String> names = projectService.getAllProjectNames();
        if (names.isEmpty()) {
            info("Delete Project", "No saved projects found.");
            return;
        }

        String currentName = projectService.getCurrentProject() != null
                ? projectService.getCurrentProject().getName()
                : null;
        String defaultName = currentName != null && names.contains(currentName)
                ? currentName
                : names.get(0);

        ChoiceDialog<String> dialog = new ChoiceDialog<>(defaultName, names);
        dialog.setTitle("Delete Project");
        dialog.setHeaderText("Select a project to delete");
        dialog.setContentText("Project:");
        styleDialog(dialog);

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String selected = result.get();
        if (!confirm("Delete Project", "Delete project '" + selected + "' and its saved results?")) {
            return;
        }

        boolean deletingCurrent = currentName != null && currentName.equals(selected);
        projectService.deleteProject(selected);

        if (deletingCurrent) {
            clearCurrentProjectView();
        }

        statusLeft.setText("Project deleted: " + selected);
        statusRight.setText("");
        updateContextLabels();
        validateForm(false);
    }

    @FXML
    private void onSaveConfig() {
        if (isRunning()) {
            return;
        }

        String defaultName = !activeConfigurationName.isBlank()
                ? activeConfigurationName
                : "New Configuration";

        javafx.scene.control.TextInputDialog dialog =
                new javafx.scene.control.TextInputDialog(defaultName);
        dialog.setTitle("Save Configuration");
        dialog.setHeaderText("Save current configuration as...");
        dialog.setContentText("Name:");
        styleDialog(dialog);

        dialog.showAndWait().ifPresent(name -> {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                info("Save Configuration", "Configuration name cannot be empty.");
                return;
            }
            try {
                Configuration cfg = readConfigurationFromForm(trimmed);
                configurationService.saveConfiguration(cfg);
                refreshConfigurationChoices(trimmed);
                activeConfigurationName = trimmed;
                configDirty = false;
                statusLeft.setText("Configuration saved: " + trimmed);
                updateContextLabels();
                validateForm(false);
            } catch (Exception e) {
                info("Error", "Could not save configuration: " + e.getMessage());
            }
        });
    }

    private Configuration showCreateConfigurationDialog() {
        return showConfigurationEditor(null);
    }

    private Configuration showConfigurationEditor(Configuration existing) {
        Dialog<Configuration> dialog = new Dialog<>();
        boolean editing = existing != null;
        dialog.setTitle(editing ? "Edit Configuration" : "New Configuration");
        dialog.setHeaderText(editing ? "Edit configuration" : "Create a new configuration");
        styleDialog(dialog);
        dialog.getDialogPane().setPrefWidth(660);

        ButtonType saveBtn = new ButtonType(editing ? "Save" : "Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.getStyleClass().add("dialog-input");
        nameField.setPromptText("e.g. Custom Java Config");
        if (editing) {
            nameField.setText(existing.getName() != null ? existing.getName() : "");
        }

        TextField languageField = new TextField();
        languageField.getStyleClass().add("dialog-input");
        languageField.setPromptText("e.g. Java, C#, Go");
        if (editing) {
            languageField.setText(existing.getLanguage() != null ? existing.getLanguage() : "");
        }

        TextField sourceFileField = new TextField();
        sourceFileField.getStyleClass().add("dialog-input");
        sourceFileField.setPromptText("e.g. Main.java, main.c, app.py");
        if (editing) {
            sourceFileField.setText(existing.getSourceFileName() != null ? existing.getSourceFileName() : "");
        }

        TextField compileCommandField = new TextField();
        compileCommandField.getStyleClass().add("dialog-input");
        compileCommandField.setPromptText("e.g. javac Main.java");
        if (editing) {
            compileCommandField.setText(existing.getCompileCommand() != null ? existing.getCompileCommand() : "");
        }

        TextField runCommandField = new TextField();
        runCommandField.getStyleClass().add("dialog-input");
        runCommandField.setPromptText("e.g. java Main");
        if (editing) {
            runCommandField.setText(existing.getRunCommand() != null ? existing.getRunCommand() : "");
        }

        for (TextField field : List.of(
                nameField,
                languageField,
                sourceFileField,
                compileCommandField,
                runCommandField
        )) {
            field.setMaxWidth(Double.MAX_VALUE);
        }

        Label validationLabel = new Label("");
        validationLabel.getStyleClass().add("validation-text");
        validationLabel.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(130);
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, fieldColumn);

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Language:"), 0, 1);
        grid.add(languageField, 1, 1);
        grid.add(new Label("Source File:"), 0, 2);
        grid.add(sourceFileField, 1, 2);
        grid.add(new Label("Compile Command:"), 0, 3);
        grid.add(compileCommandField, 1, 3);
        grid.add(new Label("Run Command:"), 0, 4);
        grid.add(runCommandField, 1, 4);
        grid.add(validationLabel, 0, 5);
        GridPane.setColumnSpan(validationLabel, 2);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            clearDialogFieldErrors(nameField, languageField, sourceFileField, runCommandField);
            String message = "";
            if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                nameField.pseudoClassStateChanged(ERROR, true);
                message = "Configuration name cannot be empty.";
            } else if (languageField.getText() == null || languageField.getText().trim().isEmpty()) {
                languageField.pseudoClassStateChanged(ERROR, true);
                message = "Language cannot be empty.";
            } else if (sourceFileField.getText() == null || sourceFileField.getText().trim().isEmpty()) {
                sourceFileField.pseudoClassStateChanged(ERROR, true);
                message = "Source file cannot be empty.";
            } else if (runCommandField.getText() == null || runCommandField.getText().trim().isEmpty()) {
                runCommandField.pseudoClassStateChanged(ERROR, true);
                message = "Run command cannot be empty.";
            }

            if (!message.isBlank()) {
                validationLabel.setText(message);
                evt.consume();
            }
        });

        dialog.setResultConverter(button -> {
            if (button == saveBtn) {
                Configuration cfg = new Configuration();
                cfg.setName(nameField.getText().trim());
                cfg.setLanguage(languageField.getText().trim());
                cfg.setSourceFileName(sourceFileField.getText().trim());
                cfg.setCompileCommand(compileCommandField.getText() != null
                        ? compileCommandField.getText().trim()
                        : "");
                cfg.setRunCommand(runCommandField.getText().trim());
                cfg.setCompiled(cfg.getCompileCommand() != null && !cfg.getCompileCommand().isBlank());
                if (editing && !existing.getName().equals(cfg.getName())) {
                    configurationService.removeConfiguration(existing.getName());
                }
                configurationService.saveConfiguration(cfg);
                return cfg;
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    @FXML
    private void onManageConfigs() {
        if (isRunning()) {
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Manage Configurations");
        dialog.setHeaderText("Saved configurations");
        styleDialog(dialog);

        ListView<String> list = new ListView<>();
        list.setItems(FXCollections.observableArrayList(configurationService.listConfigurationNames()));
        list.setPrefSize(360, 240);

        ButtonType newBtnType = new ButtonType("New...", ButtonBar.ButtonData.LEFT);
        ButtonType loadBtnType = new ButtonType("Load / Edit", ButtonBar.ButtonData.OK_DONE);
        ButtonType deleteBtnType = new ButtonType("Delete", ButtonBar.ButtonData.OTHER);
        ButtonType closeBtnType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(newBtnType, loadBtnType, deleteBtnType, closeBtnType);

        VBox box = new VBox(8, new Label("Select a configuration:"), list);
        box.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(box);

        Button newBtn = (Button) dialog.getDialogPane().lookupButton(newBtnType);
        newBtn.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            Configuration created = showCreateConfigurationDialog();
            if (created != null) {
                refreshConfigurationChoices(created.getName());
                list.setItems(FXCollections.observableArrayList(configurationService.listConfigurationNames()));
                list.getSelectionModel().select(created.getName());
                statusLeft.setText("Configuration created: " + created.getName());
            }
            evt.consume();
        });

        Button deleteBtn = (Button) dialog.getDialogPane().lookupButton(deleteBtnType);
        deleteBtn.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            String selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) {
                info("Delete Configuration", "Please select a configuration first.");
            } else if (confirm("Delete Configuration", "Delete '" + selected + "'?")) {
                configurationService.removeConfiguration(selected);
                list.getItems().remove(selected);
                refreshConfigurationChoices(null);
                statusLeft.setText("Configuration deleted: " + selected);
            }
            evt.consume();
        });

        Button loadBtn = (Button) dialog.getDialogPane().lookupButton(loadBtnType);
        loadBtn.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            String selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) {
                info("Load Configuration", "Please select a configuration first.");
                evt.consume();
                return;
            }
            Configuration cfg = configurationService.getConfiguration(selected);
            if (cfg == null) {
                info("Load Configuration", "Configuration not found.");
                evt.consume();
                return;
            }
            Configuration edited = showConfigurationEditor(cfg);
            if (edited == null) {
                evt.consume();
                return;
            }
            refreshConfigurationChoices(edited.getName());
            list.setItems(FXCollections.observableArrayList(configurationService.listConfigurationNames()));
            list.getSelectionModel().select(edited.getName());
            applyConfigurationToForm(edited);
            com.iae.model.Project current = projectService.getCurrentProject();
            if (current != null) {
                current.setConfiguration(edited);
            }
            statusLeft.setText("Configuration saved and loaded: " + edited.getName());
        });

        dialog.showAndWait();
    }

    @FXML
    private void onImportConfig() {
        if (isRunning()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Configuration");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File source = chooser.showOpenDialog(runTestsBtn.getScene().getWindow());

        if (source != null) {
            try {
                Configuration imported = configurationService.importConfiguration(source);
                configurationService.saveConfiguration(imported);
                refreshConfigurationChoices(imported.getName());
                statusLeft.setText("Configuration imported: " + imported.getName());
            } catch (Exception e) {
                info("Error", "Could not import configuration: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onExportConfig() {
        if (isRunning()) return;

        Configuration cfg = readConfigurationFromForm(activeConfigurationName.isBlank() ? "Exported_Config" : activeConfigurationName);

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Configuration");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        chooser.setInitialFileName(cfg.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".json");
        File dest = chooser.showSaveDialog(runTestsBtn.getScene().getWindow());

        if (dest != null) {
            try {
                configurationService.exportConfiguration(cfg, dest);
                statusLeft.setText("Configuration exported successfully.");
            } catch (Exception e) {
                info("Error", "Could not export configuration: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onShowManual() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("CE316 IAE User Manual");
        dialog.setHeaderText("Integrated Assignment Environment Manual");
        styleDialog(dialog);

        TextArea textArea = new TextArea(
                "Welcome to the CE316 Integrated Assignment Environment!\n\n" +
                "1. Projects:\n" +
                "   - Use 'File -> New Project' to start a new evaluation session.\n" +
                "   - 'Open Project' loads a previously saved session including results.\n\n" +
                "2. Configurations:\n" +
                "   - Select an existing configuration from the dropdown or manage them in 'Configuration -> Manage Configurations...'\n" +
                "   - You can also 'Import' and 'Export' configurations from JSON files under the 'File' menu.\n" +
                "   - A configuration requires a source file name, compile command (optional for interpreted languages), and run command.\n\n" +
                "3. Student Submissions:\n" +
                "   - Click 'Browse...' to select the folder containing student ZIP files.\n" +
                "   - The ZIP file name (without extension) is treated as the student ID.\n\n" +
                "4. Evaluation:\n" +
                "   - Select an Expected Output file (.txt or .out).\n" +
                "   - Click 'Run Tests' to evaluate all submissions. The tool will automatically extract each ZIP, compile (if needed), and run the code.\n" +
                "   - Output will be compared against the expected output.\n\n" +
                "5. Results:\n" +
                "   - Use the filter dropdown to view 'Passed', 'Failed', etc.\n" +
                "   - Double-click a row or click 'Details' to view the full compiler/runtime output for a student."
        );
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(600);
        textArea.setPrefHeight(400);

        dialog.getDialogPane().setContent(textArea);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    @FXML
    private void onAbout() {
        info("About", "CE316 Integrated Assignment Environment\nVersion 1.0.0");
    }

    @FXML
    private void onExit() {
        if (evaluationTask != null) {
            evaluationTask.cancel();
        }
        ((Stage) runTestsBtn.getScene().getWindow()).close();
    }

    @FXML
    private void onBrowseSubmissions() {
        if (isRunning()) {
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Submission Folder");
        File dir = chooser.showDialog(runTestsBtn.getScene().getWindow());
        if (dir != null) {
            submissionFolderField.setText(dir.getAbsolutePath());
        }
    }

    @FXML
    private void onBrowseExpectedOutput() {
        if (isRunning()) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Expected Output File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt", "*.out"));
        File f = chooser.showOpenDialog(runTestsBtn.getScene().getWindow());
        if (f != null) {
            expectedOutputField.setText(f.getAbsolutePath());
        }
    }

    private void loadExpectedOutputContent() {
        String path = cleanText(expectedOutputField.getText());
        if (path.isBlank()) {
            expectedOutputHintLabel.setText("No file loaded");
            expectedOutputTextArea.setText("");
            return;
        }

        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            expectedOutputHintLabel.setText("File not found: " + file.getName());
            expectedOutputTextArea.setText("");
            return;
        }

        try {
            String content = Files.readString(file.toPath());
            expectedOutputTextArea.setText(content);
            expectedOutputHintLabel.setText("Loaded: " + file.getAbsolutePath());
        } catch (Exception e) {
            expectedOutputHintLabel.setText("Could not read file: " + file.getName());
            expectedOutputTextArea.setText("");
        }
    }

    @FXML
    private void onSaveExpectedOutput() {
        if (isRunning()) return;

        String path = cleanText(expectedOutputField.getText());
        File file;

        if (path.isBlank()) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Expected Output");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt", "*.out"));
            File dest = chooser.showSaveDialog(runTestsBtn.getScene().getWindow());
            if (dest == null) {
                return;
            }
            file = dest;
            expectedOutputField.setText(file.getAbsolutePath());
        } else {
            file = new File(path);
        }

        try {
            Files.writeString(file.toPath(), expectedOutputTextArea.getText() != null ? expectedOutputTextArea.getText() : "");
            expectedOutputHintLabel.setText("Saved: " + file.getAbsolutePath());
            info("Expected Output Saved", "Successfully saved changes to expected output.");
        } catch (Exception e) {
            info("Error", "Could not save expected output: " + e.getMessage());
        }
    }

    @FXML
    private void onClearExpectedOutput() {
        if (isRunning()) return;
        
        if (confirm("Clear Expected Output", "This will remove the expected output file from the current project. Continue?")) {
            expectedOutputField.setText("");
            expectedOutputTextArea.setText("");
            expectedOutputHintLabel.setText("No file loaded");
        }
    }

    private void refreshSubmissionsTable() {
        submissionsData.clear();
        String path = cleanText(submissionFolderField.getText());

        if (path.isBlank()) {
            projectValidationLabel.setText("Choose a folder to preview ZIP files.");
            statusRight.setText("");
            updateContextLabels();
            validateForm();
            return;
        }

        File folder = new File(path);
        if (!folder.isDirectory()) {
            projectValidationLabel.setText("Folder is not available.");
            statusRight.setText("");
            updateContextLabels();
            validateForm();
            return;
        }

        List<File> zips = fileManager.discoverZipFiles(folder);
        for (File zip : zips) {
            String name = zip.getName();
            String studentId = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
            String size = humanReadableSize(zip.length());
            submissionsData.add(new SubmissionInfo(name, studentId, size));
        }

        projectValidationLabel.setText(submissionsData.isEmpty()
                ? "No ZIP files found in this folder."
                : submissionsData.size() + " ZIP file(s) ready.");
        statusRight.setText(submissionsData.size() + " submission(s) found.");
        updateContextLabels();
        validateForm();
    }

    private String humanReadableSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        return String.format("%.2f MB", mb);
    }

    @FXML
    private void onRunTests() {
        if (isRunning()) {
            return;
        }
        showValidationMessages = true;
        if (!validateForm(true)) {
            statusLeft.setText("Fix highlighted fields before running tests.");
            return;
        }

        File submissionsFolder = new File(cleanText(submissionFolderField.getText()));
        Configuration configuration = readConfigurationFromForm(configurationNameForSave("Runtime Configuration"));
        File expectedFile = new File(cleanText(expectedOutputField.getText()));
        String expectedOutput;

        try {
            expectedOutput = Files.readString(expectedFile.toPath());
        } catch (Exception e) {
            configValidationLabel.setText("Could not read expected output file: " + e.getMessage());
            statusLeft.setText("Expected output file could not be read.");
            validateForm(true);
            return;
        }

        com.iae.model.Project current = projectService.getCurrentProject();
        if (current != null) {
            current.setSubmissionFolder(submissionsFolder.getAbsolutePath());
            current.setConfiguration(configuration);
            projectDirty = true;
        }

        resultsData.clear();
        lastRunLabel.setText("Running...");
        statusLeft.setText("Evaluation started.");
        statusRight.setText("0 submissions processed.");
        startEvaluationTask(submissionsFolder, configuration, expectedOutput);
    }

    private void startEvaluationTask(File submissionsFolder, Configuration configuration, String expectedOutput) {
        setEvaluationRunning(true);

        evaluationTask = new Task<>() {
            @Override
            protected List<StudentResult> call() {
                return projectService.runEvaluation(
                        submissionsFolder,
                        configuration,
                        expectedOutput,
                        new ProjectService.EvaluationProgress() {
                            @Override
                            public void onSubmissionStarted(String studentId, int completed, int total) {
                                updateProgress(completed, total);
                                updateMessage("Processing " + studentId + " (" + (completed + 1) + " / " + total + ")");
                            }

                            @Override
                            public void onSubmissionFinished(StudentResult result, int completed, int total) {
                                Platform.runLater(() -> {
                                    addOrReplaceResult(result);
                                    statusRight.setText(completed + " / " + total + " submissions processed.");
                                });
                                updateProgress(completed, total);
                                updateMessage("Processed " + completed + " / " + total + " submissions.");
                            }
                        },
                        this::isCancelled);
            }
        };

        runProgress.progressProperty().bind(evaluationTask.progressProperty());
        currentRunLabel.textProperty().bind(evaluationTask.messageProperty());

        evaluationTask.setOnSucceeded(event -> {
            Task<List<StudentResult>> completedTask = evaluationTask;
            if (completedTask != null && completedTask.getValue() != null) {
                resultsData.setAll(completedTask.getValue());
            }
            finishEvaluationRun();
            int total = resultsData.size();
            long passed = resultsData.stream().filter(r -> r.getStatus().isPassed()).count();
            statusLeft.setText("Evaluation complete: " + passed + " / " + total + " passed.");
            statusRight.setText(total + " submissions processed.");
            lastRunLabel.setText("Completed " + timestamp());
            com.iae.model.Project current = projectService.getCurrentProject();
            if (current != null) {
                current.setResults(new ArrayList<>(resultsData));
                projectDirty = true;
                updateContextLabels();
            }
        });

        evaluationTask.setOnCancelled(event -> {
            finishEvaluationRun();
            int total = resultsData.size();
            statusLeft.setText("Evaluation cancelled.");
            statusRight.setText(total + " submissions processed before cancellation.");
            lastRunLabel.setText("Cancelled " + timestamp());
            com.iae.model.Project current = projectService.getCurrentProject();
            if (current != null) {
                current.setResults(new ArrayList<>(resultsData));
                projectDirty = true;
                updateContextLabels();
            }
        });

        evaluationTask.setOnFailed(event -> {
            Throwable error = evaluationTask.getException();
            finishEvaluationRun();
            statusLeft.setText("Evaluation failed.");
            lastRunLabel.setText("Failed " + timestamp());
            info("Execution Error", error != null && error.getMessage() != null
                    ? error.getMessage()
                    : "Evaluation failed unexpectedly.");
        });

        Thread worker = new Thread(evaluationTask, "evaluation-worker");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void onCancelRun() {
        if (evaluationTask != null) {
            statusLeft.setText("Cancelling evaluation...");
            evaluationTask.cancel();
        }
    }

    private void finishEvaluationRun() {
        runProgress.progressProperty().unbind();
        currentRunLabel.textProperty().unbind();
        currentRunLabel.setText("");
        runProgress.setProgress(0);
        evaluationTask = null;
        setEvaluationRunning(false);
        updateResultsSummary();
        updateContextLabels();
        validateForm();
    }

    private void setEvaluationRunning(boolean running) {
        setManagedVisible(runProgress, running);
        setManagedVisible(cancelRunBtn, running);
        cancelRunBtn.setDisable(!running);

        newProjectBtn.setDisable(running);
        openProjectBtn.setDisable(running);
        saveProjectBtn.setDisable(running);
        deleteProjectBtn.setDisable(running);
        browseSubmissionsBtn.setDisable(running);
        browseExpectedOutputBtn.setDisable(running);
        saveConfigBtn.setDisable(running);
        configCombo.setDisable(running);
        sourceFileField.setDisable(running);
        compileCmdField.setDisable(running);
        runCmdField.setDisable(running);
        expectedOutputField.setDisable(running);
        submissionFolderField.setDisable(running);
        runTestsBtn.setDisable(running || !validateForm(showValidationMessages));
    }

    private void setManagedVisible(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private boolean validateForm() {
        return validateForm(showValidationMessages);
    }

    private boolean validateForm(boolean showMessages) {
        if (showMessages) {
            showValidationMessages = true;
        }

        boolean folderError = false;
        boolean configError = false;
        List<String> folderMessages = new ArrayList<>();
        List<String> configMessages = new ArrayList<>();

        String folderPath = cleanText(submissionFolderField.getText());
        if (folderPath.isBlank()) {
            folderError = true;
            folderMessages.add("Select a folder containing student ZIP files.");
        } else {
            File folder = new File(folderPath);
            if (!folder.isDirectory()) {
                folderError = true;
                folderMessages.add("Submission folder does not exist.");
            } else if (submissionsData.isEmpty()) {
                folderError = true;
                folderMessages.add("No ZIP files were found in this folder.");
            }
        }

        if (configCombo.getValue() == null || configCombo.getValue().isBlank()) {
            configError = true;
            configMessages.add("Select a saved configuration.");
        }
        if (activeConfigurationLanguage.isBlank()) {
            configError = true;
            configMessages.add("Configuration language is missing.");
        }
        if (sourceFileField.getText() == null || sourceFileField.getText().isBlank()) {
            configError = true;
            configMessages.add("Enter the source file name.");
        }
        if (runCmdField.getText() == null || runCmdField.getText().isBlank()) {
            configError = true;
            configMessages.add("Enter the run command.");
        }

        String expectedPath = cleanText(expectedOutputField.getText());
        if (expectedPath.isBlank()) {
            configError = true;
            configMessages.add("Select an expected output file.");
        } else if (!new File(expectedPath).isFile()) {
            configError = true;
            configMessages.add("Expected output file was not found.");
        }

        projectValidationLabel.setText(showValidationMessages ? String.join(" ", folderMessages) : "");
        configValidationLabel.setText(showValidationMessages ? String.join(" ", configMessages) : "");

        submissionFolderField.pseudoClassStateChanged(ERROR, showValidationMessages && folderError);
        configCombo.pseudoClassStateChanged(ERROR, showValidationMessages
                && (configCombo.getValue() == null || configCombo.getValue().isBlank()));
        sourceFileField.pseudoClassStateChanged(ERROR, showValidationMessages
                && (sourceFileField.getText() == null || sourceFileField.getText().isBlank()));
        runCmdField.pseudoClassStateChanged(ERROR, showValidationMessages
                && (runCmdField.getText() == null || runCmdField.getText().isBlank()));
        expectedOutputField.pseudoClassStateChanged(ERROR, showValidationMessages
                && (expectedPath.isBlank() || !new File(expectedPath).isFile()));

        boolean valid = !folderError && !configError;
        if (evaluationTask == null) {
            runTestsBtn.setDisable(!valid);
        }
        updateRunTooltip(valid, folderMessages, configMessages);
        return valid;
    }

    private void addOrReplaceResult(StudentResult result) {
        for (int i = 0; i < resultsData.size(); i++) {
            StudentResult existing = resultsData.get(i);
            if (existing.getStudentId() != null && existing.getStudentId().equals(result.getStudentId())) {
                resultsData.set(i, result);
                return;
            }
        }
        resultsData.add(result);
    }

    private void loadSelectedConfiguration() {
        String selectedName = configCombo.getValue();
        if (selectedName == null || selectedName.isBlank()) {
            return;
        }
        Configuration selected = configurationService.getConfiguration(selectedName);
        if (selected == null && selectedName.equals(activeConfigurationName)) {
            selected = readConfigurationFromForm(selectedName);
        }
        if (selected != null) {
            applyConfigurationToForm(selected);
            markProjectDirty();
            statusLeft.setText("Configuration selected: " + selectedName);
        }
    }

    private void refreshConfigurationChoices(String preferredName) {
        String selectedName = preferredName != null ? preferredName : configCombo.getValue();
        List<String> names = configurationService.listConfigurationNames();

        suppressConfigurationListener = true;
        try {
            configCombo.setItems(FXCollections.observableArrayList(names));
            if (selectedName != null && names.contains(selectedName)) {
                configCombo.getSelectionModel().select(selectedName);
            } else if (!names.isEmpty()) {
                configCombo.getSelectionModel().selectFirst();
            } else {
                configCombo.getSelectionModel().clearSelection();
                configCombo.setValue(null);
                activeConfigurationName = "";
                activeConfigurationLanguage = "";
                configLanguageLabel.setText("No language specified");
                sourceFileField.setText("");
                compileCmdField.setText("");
                runCmdField.setText("");
                expectedOutputField.setText("");
            }
        } finally {
            suppressConfigurationListener = false;
        }

        if (configCombo.getValue() != null) {
            Configuration selected = configurationService.getConfiguration(configCombo.getValue());
            if (selected != null) {
                applyConfigurationToForm(selected);
            }
        }
    }

    private void clearCurrentProjectView() {
        suppressFormListeners = true;
        submissionFolderField.setText("");
        expectedOutputField.setText("");
        sourceFileField.setText("");
        compileCmdField.setText("");
        runCmdField.setText("");
        suppressFormListeners = false;
        resultsData.clear();
        submissionsData.clear();
        lastRunLabel.setText("Not run yet");
        projectDirty = false;
        configDirty = false;
        showValidationMessages = false;
        refreshConfigurationChoices(null);
        updateResultsSummary();
        updateContextLabels();
        validateForm(false);
    }

    private void applyResultsFilter() {
        String selected = statusFilterCombo.getValue();
        filteredResultsData.setPredicate(result -> {
            if (selected == null || FILTER_ALL.equals(selected)) {
                return true;
            }
            if (FILTER_PASSED.equals(selected)) {
                return result.getStatus().isPassed();
            }
            if (FILTER_FAILED.equals(selected)) {
                return !result.getStatus().isPassed();
            }
            return result.getStatus().display().equals(selected);
        });
    }

    private void updateResultsSummary() {
        int total = resultsData.size();
        if (total == 0) {
            resultsSummaryLabel.setText("No results yet");
            return;
        }

        int shown = filteredResultsData.size();
        long passed = filteredResultsData.stream().filter(r -> r.getStatus().isPassed()).count();
        long failed = shown - passed;
        long timeout = filteredResultsData.stream().filter(r -> r.getStatus() == TestStatus.TIMEOUT).count();
        long compile = filteredResultsData.stream().filter(r -> r.getStatus() == TestStatus.COMPILATION_ERROR).count();
        String prefix = shown == total ? total + " total" : shown + " of " + total + " shown";
        resultsSummaryLabel.setText(prefix + " - " + passed + " passed - " + failed
                + " failed - " + compile + " compile - " + timeout + " timeout");
    }

    private void updateContextLabels() {
        com.iae.model.Project project = projectService.getCurrentProject();
        String projectName = project != null && project.getName() != null
                ? project.getName() + (projectDirty ? " *" : "")
                : "No active project";
        setLabelTextWithTooltip(projectNameLabel, projectName);

        String configName = !activeConfigurationName.isBlank()
                ? activeConfigurationName + (configDirty ? " *" : "")
                : "No configuration selected";
        setLabelTextWithTooltip(configNameLabel, configName);
        setLabelTextWithTooltip(submissionCountLabel, submissionsData.size() + " ZIP file(s)");
    }

    private String detailsText(StudentResult result) {
        String details = cleanText(result.getDetails());
        if (!details.isBlank()) {
            return details;
        }
        if (result.getStatus() != null && result.getStatus().isPassed()) {
            return "Output matched expected output.";
        }
        return "No details recorded.";
    }

    private String statusStyleClass(TestStatus status) {
        if (status == null) {
            return "status-pending";
        }
        return switch (status) {
            case PASSED -> "status-passed";
            case COMPILATION_ERROR -> "status-compile";
            case RUNTIME_ERROR -> "status-runtime";
            case TIMEOUT -> "status-timeout";
            case OUTPUT_MISMATCH -> "status-mismatch";
            case EXTRACTION_ERROR, MISSING_SOURCE -> "status-missing";
            case PENDING -> "status-pending";
            default -> "status-failed";
        };
    }

    private boolean isRunning() {
        return evaluationTask != null;
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String cleanText(String text) {
        return text == null ? "" : text.trim();
    }

    private void markFormEdited() {
        if (!formInitialized || suppressFormListeners) {
            return;
        }
        showValidationMessages = true;
        markProjectDirty();
    }

    private void markConfigurationEdited() {
        if (!formInitialized || suppressFormListeners) {
            return;
        }
        showValidationMessages = true;
        configDirty = true;
        markProjectDirty();
    }

    private void markProjectDirty() {
        if (projectService.getCurrentProject() != null) {
            projectDirty = true;
        }
        updateContextLabels();
    }

    private String configurationNameForSave(String fallback) {
        return !activeConfigurationName.isBlank() ? activeConfigurationName : fallback;
    }

    private void updateRunTooltip(boolean valid, List<String> folderMessages, List<String> configMessages) {
        if (valid) {
            runTestsBtn.setTooltip(new Tooltip("Run the active configuration against discovered submissions."));
            return;
        }
        List<String> reasons = new ArrayList<>();
        reasons.addAll(folderMessages);
        reasons.addAll(configMessages);
        String reason = reasons.isEmpty()
                ? "Complete the project inputs before running tests."
                : reasons.get(0);
        runTestsBtn.setTooltip(new Tooltip(reason));
    }

    private void updateResultsPlaceholder() {
        if (resultsData.isEmpty()) {
            resultsTable.setPlaceholder(new Label("Run tests to see per-student results."));
        } else if (filteredResultsData.isEmpty()) {
            resultsTable.setPlaceholder(new Label("No results match the selected filter."));
        }
    }

    private void setLabelTextWithTooltip(Label label, String text) {
        label.setText(text);
        label.setTooltip(new Tooltip(text));
    }

    private void clearDialogFieldErrors(TextField... fields) {
        for (TextField field : fields) {
            field.pseudoClassStateChanged(ERROR, false);
        }
    }

    private void styleDialog(Dialog<?> dialog) {
        String stylesheet = getClass().getResource("/css/styles.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(stylesheet);
        dialog.getDialogPane().getStyleClass().add("app-dialog");
    }

    private void styleAlert(Alert alert) {
        String stylesheet = getClass().getResource("/css/styles.css").toExternalForm();
        alert.getDialogPane().getStylesheets().add(stylesheet);
        alert.getDialogPane().getStyleClass().add("app-dialog");
    }

    private void info(String title, String body) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, body, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(title);
        styleAlert(a);
        a.showAndWait();
    }

    private boolean confirm(String title, String body) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, body, ButtonType.OK, ButtonType.CANCEL);
        a.setTitle(title);
        a.setHeaderText(title);
        styleAlert(a);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private void showResultDetailsDialog(StudentResult result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Student Result Details");
        alert.setHeaderText("Result for Student: " + result.getStudentId()
                + "\nStatus: " + result.getStatus().display());
        styleAlert(alert);

        TextArea textArea = new TextArea(detailsText(result));
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(560);
        textArea.setPrefHeight(320);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }
}

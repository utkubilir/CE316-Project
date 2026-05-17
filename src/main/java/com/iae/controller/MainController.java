package com.iae.controller;

import com.iae.model.Configuration;
import com.iae.model.StudentResult;
import com.iae.model.SubmissionInfo;
import com.iae.model.TestStatus;
import com.iae.service.ConfigurationService;
import com.iae.service.FileManager;
import com.iae.service.ProjectService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainController {

    @FXML private TextField submissionFolderField;
    @FXML private ComboBox<String> languageCombo;
    @FXML private TextField sourceFileField;
    @FXML private TextField compileCmdField;
    @FXML private TextField runCmdField;
    @FXML private TextField expectedOutputField;
    @FXML private Button runTestsBtn;

    @FXML private TableView<StudentResult> resultsTable;
    @FXML private TableColumn<StudentResult, String> colStudentId;
    @FXML private TableColumn<StudentResult, TestStatus> colStatus;
    @FXML private TableColumn<StudentResult, String> colDetails;

    @FXML private TableView<SubmissionInfo> submissionsTable;
    @FXML private TableColumn<SubmissionInfo, String> colSubZip;
    @FXML private TableColumn<SubmissionInfo, String> colSubStudent;
    @FXML private TableColumn<SubmissionInfo, String> colSubSize;

    @FXML private Label statusLeft;
    @FXML private Label statusRight;

    private final ObservableList<StudentResult> resultsData = FXCollections.observableArrayList();
    private final ObservableList<SubmissionInfo> submissionsData = FXCollections.observableArrayList();
    private final ProjectService projectService = new ProjectService();
    private final ConfigurationService configurationService = new ConfigurationService();
    private final FileManager fileManager = new FileManager();

    /**
     * Guards against the language ComboBox listener overwriting fields that
     * we set programmatically (e.g. when loading a saved configuration into
     * the form).
     */
    private boolean suppressLanguageListener = false;

    @FXML
    public void initialize() {
        // Seed default Java/C/C++/Python configurations on first run so the
        // Manage Configurations dialog is never empty. (Requirement #4)
        configurationService.seedDefaultsIfEmpty();

        refreshLanguageChoices();
        languageCombo.setOnAction(event -> {
            if (!suppressLanguageListener) applyLanguageDefaults();
        });

        // Start from the first saved configuration language.
        applyLanguageDefaults();
        expectedOutputField.setText("");
        submissionFolderField.setText("");

        // ----- Results table (Requirement #9) -----
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colStatus.setCellFactory(column -> new TableCell<StudentResult, TestStatus>() {
            @Override
            protected void updateItem(TestStatus item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-passed", "status-failed", "status-pending");
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.display());
                    if (item.isPassed()) getStyleClass().add("status-passed");
                    else if (item == TestStatus.PENDING) getStyleClass().add("status-pending");
                    else getStyleClass().add("status-failed");
                }
            }
        });

        resultsTable.setItems(resultsData);

        resultsTable.setRowFactory(tv -> {
            TableRow<StudentResult> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showResultDetailsDialog(row.getItem());
                }
            });
            return row;
        });

        // ----- Submissions table (supports Requirement #9 preview) -----
        colSubZip.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        colSubStudent.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colSubSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        submissionsTable.setItems(submissionsData);

        // Refresh the submissions table whenever the folder path changes,
        // regardless of whether it was typed or set via the Browse button.
        submissionFolderField.textProperty().addListener(
                (obs, oldVal, newVal) -> refreshSubmissionsTable());

        statusLeft.setText("Ready.");
        statusRight.setText("");
    }

    // ---------------------------------------------------------------------
    // Requirement #3: create a project that uses an existing or new
    // configuration. After the user supplies a project name we let them pick
    // a saved configuration (if any) or start from a language template.
    // ---------------------------------------------------------------------
    @FXML
    private void onNewProject() {
        TextInputDialog dialog = new TextInputDialog("New Project");
        dialog.setTitle("New Project");
        dialog.setHeaderText("Create a New Project");
        dialog.setContentText("Please enter project name:");

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
        statusLeft.setText("New project created: " + name);
    }

    /**
     * Lets the user pick an existing saved configuration or start a new one
     * from a language template.
     */
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
        if (c == null) return;
        suppressLanguageListener = true;
        try {
            if (c.getLanguage() != null) languageCombo.setValue(c.getLanguage());
            sourceFileField.setText(c.getSourceFileName() != null ? c.getSourceFileName() : "");
            compileCmdField.setText(c.getCompileCommand() != null ? c.getCompileCommand() : "");
            runCmdField.setText(c.getRunCommand() != null ? c.getRunCommand() : "");
            if (c.getExpectedOutputPath() != null) {
                expectedOutputField.setText(c.getExpectedOutputPath());
            }
        } finally {
            suppressLanguageListener = false;
        }
    }

    private Configuration readConfigurationFromForm(String name) {
        String language = languageCombo.getValue();
        Configuration cfg = new Configuration(name, language);
        cfg.setSourceFileName(sourceFileField.getText());
        cfg.setCompileCommand(compileCmdField.getText());
        cfg.setRunCommand(runCmdField.getText());
        cfg.setExpectedOutputPath(expectedOutputField.getText());
        cfg.setCompiled(cfg.getCompileCommand() != null && !cfg.getCompileCommand().isBlank());
        return cfg;
    }

    @FXML
    private void onOpenProject() {
        List<String> names = projectService.getAllProjectNames();
        if (names.isEmpty()) {
            info("Open Project", "No saved projects found.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(names.get(0), names);
        dialog.setTitle("Open Project");
        dialog.setHeaderText("Select a project to open");
        dialog.setContentText("Project:");

        dialog.showAndWait().ifPresent(name -> {
            com.iae.model.Project p = projectService.loadProject(name);
            if (p != null) {
                projectService.setCurrentProject(p);
                submissionFolderField.setText(p.getSubmissionFolder() != null ? p.getSubmissionFolder() : "");
                if (p.getConfiguration() != null) {
                    applyConfigurationToForm(p.getConfiguration());
                }
                resultsData.clear();
                if (p.getResults() != null) {
                    resultsData.addAll(p.getResults());
                }
                statusLeft.setText("Project loaded: " + name);
            } else {
                info("Error", "Could not load project.");
            }
        });
    }

    @FXML
    private void onSaveProject() {
        com.iae.model.Project p = projectService.getCurrentProject();
        if (p == null) {
            info("Save Project", "No active project to save. Please create or open a project first.");
            return;
        }

        p.setSubmissionFolder(submissionFolderField.getText());

        Configuration c = p.getConfiguration();
        if (c == null) {
            c = new Configuration();
            p.setConfiguration(c);
        }
        c.setName(p.getName() + " Config");
        c.setLanguage(languageCombo.getValue());
        c.setSourceFileName(sourceFileField.getText());
        c.setCompileCommand(compileCmdField.getText());
        c.setRunCommand(runCmdField.getText());
        c.setExpectedOutputPath(expectedOutputField.getText());

        p.setResults(new ArrayList<>(resultsData));

        try {
            projectService.saveProject(p);
            statusLeft.setText("Project saved successfully.");
        } catch (Exception e) {
            info("Error", "Failed to save project: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteProject() {
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
    }

    @FXML private void onImportConfig() { info("Import Configuration", "Out of scope for Milestone 2."); }
    @FXML private void onExportConfig() { info("Export Configuration", "Out of scope for Milestone 2."); }

    // ---------------------------------------------------------------------
    // Requirement #4: create, edit and remove a configuration.
    // ---------------------------------------------------------------------
    @FXML
    private void onSaveConfig() {
        String currentLang = languageCombo.getValue();
        String defaultName = currentLang != null ? currentLang + " Configuration" : "New Configuration";

        TextInputDialog dialog = new TextInputDialog(defaultName);
        dialog.setTitle("Save Configuration");
        dialog.setHeaderText("Save current configuration as...");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(name -> {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                info("Save Configuration", "Configuration name cannot be empty.");
                return;
            }
            try {
                Configuration cfg = readConfigurationFromForm(trimmed);
                configurationService.saveConfiguration(cfg);
                refreshLanguageChoices();
                statusLeft.setText("Configuration saved: " + trimmed);
            } catch (Exception e) {
                info("Error", "Could not save configuration: " + e.getMessage());
            }
        });
    }

    /**
     * Create-new-configuration dialog. Lets the user define the configuration
     * directly instead of choosing from built-in language presets.
     */
    private Configuration showCreateConfigurationDialog() {
        return showConfigurationEditor(null);
    }

    private Configuration showConfigurationEditor(Configuration existing) {
        Dialog<Configuration> dialog = new Dialog<>();
        boolean editing = existing != null;
        dialog.setTitle(editing ? "Edit Configuration" : "New Configuration");
        dialog.setHeaderText(editing ? "Edit configuration" : "Create a new configuration");

        ButtonType saveBtn = new ButtonType(editing ? "Save" : "Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Custom Java Config");
        if (editing) nameField.setText(existing.getName());

        TextField languageField = new TextField();
        languageField.setPromptText("e.g. Java, C#, Go");
        if (editing) languageField.setText(existing.getLanguage());

        TextField sourceFileField = new TextField();
        sourceFileField.setPromptText("e.g. Main.java, main.c, app.py");
        if (editing) sourceFileField.setText(existing.getSourceFileName());

        TextField compileCommandField = new TextField();
        compileCommandField.setPromptText("e.g. javac Main.java");
        if (editing) compileCommandField.setText(existing.getCompileCommand());

        TextField runCommandField = new TextField();
        runCommandField.setPromptText("e.g. java Main");
        if (editing) runCommandField.setText(existing.getRunCommand());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
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
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                info(dialog.getTitle(), "Configuration name cannot be empty.");
                evt.consume();
            } else if (languageField.getText() == null || languageField.getText().trim().isEmpty()) {
                info(dialog.getTitle(), "Language cannot be empty.");
                evt.consume();
            } else if (sourceFileField.getText() == null || sourceFileField.getText().trim().isEmpty()) {
                info(dialog.getTitle(), "Source file cannot be empty.");
                evt.consume();
            } else if (runCommandField.getText() == null || runCommandField.getText().trim().isEmpty()) {
                info(dialog.getTitle(), "Run command cannot be empty.");
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
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Manage Configurations");
        dialog.setHeaderText("Saved configurations");

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

        // New
        Button newBtn = (Button) dialog.getDialogPane().lookupButton(newBtnType);
        newBtn.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            Configuration created = showCreateConfigurationDialog();
            if (created != null) {
                refreshLanguageChoices();
                list.setItems(FXCollections.observableArrayList(configurationService.listConfigurationNames()));
                list.getSelectionModel().select(created.getName());
                statusLeft.setText("Configuration created: " + created.getName());
            }
            evt.consume();
        });

        // Delete
        Button deleteBtn = (Button) dialog.getDialogPane().lookupButton(deleteBtnType);
        deleteBtn.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            String selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) {
                info("Delete Configuration", "Please select a configuration first.");
            } else if (confirm("Delete Configuration", "Delete '" + selected + "'?")) {
                configurationService.removeConfiguration(selected);
                list.getItems().remove(selected);
                refreshLanguageChoices();
                statusLeft.setText("Configuration deleted: " + selected);
            }
            evt.consume();
        });

        // Load / Edit
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
            refreshLanguageChoices();
            list.setItems(FXCollections.observableArrayList(configurationService.listConfigurationNames()));
            list.getSelectionModel().select(edited.getName());
            applyConfigurationToForm(edited);
            com.iae.model.Project current = projectService.getCurrentProject();
            if (current != null) current.setConfiguration(edited);
            statusLeft.setText("Configuration loaded: " + selected
                    + " — edit fields and use 'Save Config' to update.");
        });

        dialog.showAndWait();
    }

    @FXML private void onShowManual() { info("Manual", "Out of scope for Milestone 2."); }
    @FXML private void onAbout() { info("About", "CE316 Integrated Assignment Environment\nVersion 1.0.0"); }
    @FXML private void onExit() { ((Stage) runTestsBtn.getScene().getWindow()).close(); }

    @FXML
    private void onBrowseSubmissions() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Submission Folder");
        File dir = chooser.showDialog(runTestsBtn.getScene().getWindow());
        if (dir != null) {
            // Triggers the text listener which refreshes the submissions table.
            submissionFolderField.setText(dir.getAbsolutePath());
        }
    }

    @FXML
    private void onBrowseExpectedOutput() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Expected Output File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt", "*.out"));
        File f = chooser.showOpenDialog(runTestsBtn.getScene().getWindow());
        if (f != null) expectedOutputField.setText(f.getAbsolutePath());
    }

    /** Scan the current submissions folder and refresh the Student Submissions table. */
    private void refreshSubmissionsTable() {
        submissionsData.clear();
        String path = submissionFolderField.getText();
        if (path == null || path.isBlank()) return;

        File folder = new File(path);
        if (!folder.isDirectory()) return;

        List<File> zips = fileManager.discoverZipFiles(folder);
        for (File zip : zips) {
            String name = zip.getName();
            String studentId = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
            String size = humanReadableSize(zip.length());
            submissionsData.add(new SubmissionInfo(name, studentId, size));
        }
        statusRight.setText(submissionsData.size() + " submission(s) found.");
    }

    private String humanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        return String.format("%.2f MB", mb);
    }

    // ---------------------------------------------------------------------
    // Requirements #7 (compile/run), #8 (output compare), #9 (display).
    // ---------------------------------------------------------------------
    @FXML
    private void onRunTests() {
        try {
            File submissionsFolder = new File(submissionFolderField.getText());
            if (!submissionsFolder.exists() || !submissionsFolder.isDirectory()) {
                info("Error", "Submission folder does not exist or is not a folder.");
                return;
            }

            String lang = languageCombo.getValue();
            if (lang == null || lang.isBlank()) {
                info("Error", "Please select a language.");
                return;
            }

            if (sourceFileField.getText() == null || sourceFileField.getText().isBlank()) {
                info("Error", "Please enter the source file name.");
                return;
            }

            if (runCmdField.getText() == null || runCmdField.getText().isBlank()) {
                info("Error", "Please enter the run command.");
                return;
            }

            Configuration configuration = readConfigurationFromForm(lang + " Configuration");

            String expectedPath = expectedOutputField.getText();
            if (expectedPath == null || expectedPath.isBlank()) {
                info("Error", "Please select an expected output file.");
                return;
            }
            File expectedFile = new File(expectedPath);
            if (!expectedFile.isFile()) {
                info("Error", "Expected output file not found:\n" + expectedPath);
                return;
            }
            String expectedOutput = Files.readString(expectedFile.toPath());

            resultsData.clear();
            List<StudentResult> results = projectService.runEvaluation(
                    submissionsFolder, configuration, expectedOutput);
            resultsData.addAll(results);

            long passed = results.stream().filter(r -> r.getStatus().isPassed()).count();
            statusLeft.setText("Evaluation complete: " + passed + " / " + results.size() + " passed.");
            statusRight.setText(results.size() + " submissions processed.");

        } catch (Exception e) {
            info("Execution Error", e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private void applyLanguageDefaults() {
        String language = languageCombo.getValue();
        if (language == null) return;
        Configuration template = configurationService.getFirstConfigurationByLanguage(language);
        if (template == null) {
            template = configurationService.createConfiguration(language + " Configuration", language);
        }
        sourceFileField.setText(template.getSourceFileName() != null ? template.getSourceFileName() : "");
        compileCmdField.setText(template.getCompileCommand());
        runCmdField.setText(template.getRunCommand());
    }

    private void refreshLanguageChoices() {
        String selectedLanguage = languageCombo.getValue();
        List<String> languages = configurationService.listConfiguredLanguages();

        suppressLanguageListener = true;
        try {
            languageCombo.setItems(FXCollections.observableArrayList(languages));
            if (selectedLanguage != null && languages.contains(selectedLanguage)) {
                languageCombo.getSelectionModel().select(selectedLanguage);
            } else if (!languages.isEmpty()) {
                languageCombo.getSelectionModel().selectFirst();
            } else {
                languageCombo.getSelectionModel().clearSelection();
                languageCombo.setValue(null);
                sourceFileField.setText("");
                compileCmdField.setText("");
                runCmdField.setText("");
            }
        } finally {
            suppressLanguageListener = false;
        }

        if (languageCombo.getValue() != null) {
            applyLanguageDefaults();
        }
    }

    private void clearCurrentProjectView() {
        submissionFolderField.setText("");
        expectedOutputField.setText("");
        sourceFileField.setText("");
        resultsData.clear();
        submissionsData.clear();
        refreshLanguageChoices();
    }

    private void info(String title, String body) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, body, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(title);
        a.showAndWait();
    }

    private boolean confirm(String title, String body) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, body, ButtonType.OK, ButtonType.CANCEL);
        a.setTitle(title);
        a.setHeaderText(title);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private void showResultDetailsDialog(StudentResult result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Student Result Details");
        alert.setHeaderText("Result for Student: " + result.getStudentId()
                + "\nStatus: " + result.getStatus().display());

        TextArea textArea = new TextArea(result.getDetails());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(500);
        textArea.setPrefHeight(300);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }
}

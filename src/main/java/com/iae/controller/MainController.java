package com.iae.controller;

import com.iae.model.Configuration;
import com.iae.model.StudentResult;
import com.iae.model.TestStatus;
import com.iae.service.ConfigurationService;
import com.iae.service.ProjectService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;

public class MainController {

    @FXML private TextField submissionFolderField;
    @FXML private ComboBox<String> languageCombo;
    @FXML private TextField compileCmdField;
    @FXML private TextField runCmdField;
    @FXML private TextField expectedOutputField;
    @FXML private Button runTestsBtn;

    @FXML private TableView<StudentResult> resultsTable;
    @FXML private TableColumn<StudentResult, String> colStudentId;
    @FXML private TableColumn<StudentResult, TestStatus> colStatus;
    @FXML private TableColumn<StudentResult, String> colDetails;

    @FXML private TableView<Object> submissionsTable;
    @FXML private TableColumn<Object, String> colSubZip;
    @FXML private TableColumn<Object, String> colSubStudent;
    @FXML private TableColumn<Object, String> colSubSize;

    @FXML private Label statusLeft;
    @FXML private Label statusRight;

    private final ObservableList<StudentResult> resultsData = FXCollections.observableArrayList();
    private final ProjectService projectService =new ProjectService();
    private final ConfigurationService configurationService = new ConfigurationService();
    @FXML
    public void initialize() {
        languageCombo.setItems(FXCollections.observableArrayList("C", "C++", "Java", "Python"));
        languageCombo.getSelectionModel().select("C");
        languageCombo.setOnAction(event -> applyLanguageDefaults());

        compileCmdField.setText("/usr/bin/gcc main.c -o main");
        runCmdField.setText("./main arg1 arg2 arg3");
        expectedOutputField.setText("expected_output.txt");
        submissionFolderField.setText("/path/to/student_submissions");

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
                if (event.getClickCount() == 2 && (!row.isEmpty()) ) {
                    StudentResult rowData = row.getItem();
                    showResultDetailsDialog(rowData);
                }
            });
            return row;
        });

        loadMockResults();

        statusLeft.setText("Ready.");
        statusRight.setText("");
    }

    private void loadMockResults() {
        resultsData.addAll(
                new StudentResult("2021001", TestStatus.PASSED, ""),
                new StudentResult("2021002", TestStatus.COMPILATION_ERROR, "GCC Error: missing semicolon"),
                new StudentResult("2021003", TestStatus.PASSED, ""),
                new StudentResult("2021004", TestStatus.OUTPUT_MISMATCH,
                        "Output Mismatch\nExpected: apple, banana, cherry\nFound: apple, cherry, banana"),
                new StudentResult("2021005", TestStatus.PASSED, ""),
                new StudentResult("2021006", TestStatus.RUNTIME_ERROR, "Runtime Error\nSegmentation fault")
        );
        statusLeft.setText("Processing Complete, " + resultsData.size() + " Students Tested.");
        statusRight.setText("Reports saved to /results/report.log");
    }

    @FXML
    private void onNewProject() {
        TextInputDialog dialog = new TextInputDialog("New Project");
        dialog.setTitle("New Project");
        dialog.setHeaderText("Create a New Project");
        dialog.setContentText("Please enter project name:");

        dialog.showAndWait().ifPresent(name -> {
            com.iae.model.Project p = new com.iae.model.Project();
            p.setName(name);
            projectService.setCurrentProject(p);
            
            submissionFolderField.setText("");
            resultsData.clear();
            statusLeft.setText("New project created: " + name);
        });
    }

    @FXML
    private void onOpenProject() {
        java.util.List<String> names = projectService.getAllProjectNames();
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
                    com.iae.model.Configuration c = p.getConfiguration();
                    if (c.getLanguage() != null) languageCombo.setValue(c.getLanguage());
                    compileCmdField.setText(c.getCompileCommand() != null ? c.getCompileCommand() : "");
                    runCmdField.setText(c.getRunCommand() != null ? c.getRunCommand() : "");
                    expectedOutputField.setText(c.getExpectedOutputPath() != null ? c.getExpectedOutputPath() : "");
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
        
        com.iae.model.Configuration c = p.getConfiguration();
        if (c == null) {
            c = new com.iae.model.Configuration();
            p.setConfiguration(c);
        }
        c.setName(p.getName() + " Config");
        c.setLanguage(languageCombo.getValue());
        c.setCompileCommand(compileCmdField.getText());
        c.setRunCommand(runCmdField.getText());
        c.setExpectedOutputPath(expectedOutputField.getText());
        
        p.setResults(new java.util.ArrayList<>(resultsData));

        try {
            projectService.saveProject(p);
            statusLeft.setText("Project saved successfully.");
        } catch (Exception e) {
            info("Error", "Failed to save project: " + e.getMessage());
        }
    }
    @FXML private void onImportConfig() { info("Import Configuration", "Stub — will import a .cfg.json file."); }
    @FXML private void onExportConfig() { info("Export Configuration", "Stub — will export current config to a file."); }
    @FXML private void onManageConfigs() { info("Manage Configurations", "Stub — dialog coming soon."); }
    @FXML private void onSaveConfig() { info("Save Configuration", "Stub — persists configuration fields."); }
    @FXML private void onShowManual() { info("Manual", "Stub — will open the HTML manual."); }
    @FXML private void onAbout() { info("About", "CE316 Integrated Assignment Environment\nVersion 1.0.0"); }
    @FXML private void onExit() { ((Stage) runTestsBtn.getScene().getWindow()).close(); }

    @FXML
    private void onBrowseSubmissions() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Submission Folder");
        File dir = chooser.showDialog(runTestsBtn.getScene().getWindow());
        if (dir != null) submissionFolderField.setText(dir.getAbsolutePath());
    }

    @FXML
    private void onBrowseExpectedOutput() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Expected Output File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt", "*.out"));
        File f = chooser.showOpenDialog(runTestsBtn.getScene().getWindow());
        if (f != null) expectedOutputField.setText(f.getAbsolutePath());
    }

    @FXML
    private void onRunTests() {

        try {

        File submissionsFolder =
                new File(submissionFolderField.getText());

        if (!submissionsFolder.exists()) {

            info(
                    "Error",
                    "Submission folder does not exist."
            );

            return;
        }

        Configuration configuration =
                configurationService.createConfiguration(
                        languageCombo.getValue() + " Configuration",
                        languageCombo.getValue()
                );

        configuration.setCompileCommand(
                compileCmdField.getText()
        );

        configuration.setRunCommand(
                runCmdField.getText()
        );

        String expectedOutput =
                Files.readString(
                        new File(
                                expectedOutputField.getText()
                        ).toPath()
                );

        resultsData.clear();

        resultsData.addAll(
                projectService.runEvaluation(
                        submissionsFolder,
                        configuration,
                        expectedOutput
                )
        );

        statusLeft.setText(
                "Evaluation Complete."
        );

        statusRight.setText(
                resultsData.size()
                        + " submissions processed."
        );

    } catch (Exception e) {

        info(
                "Execution Error",
                e.getMessage()
        );
    }
}
    private void applyLanguageDefaults() {

        String language = languageCombo.getValue();

        Configuration config = configurationService.createConfiguration(
                language + " Configuration",
                language
        );

        compileCmdField.setText(config.getCompileCommand());
        runCmdField.setText(config.getRunCommand());
    }
    private void info(String title, String body) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, body, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(title);
        a.showAndWait();
    }

    private void showResultDetailsDialog(StudentResult result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Student Result Details");
        alert.setHeaderText("Result for Student: " + result.getStudentId() + "\nStatus: " + result.getStatus().display());
        
        TextArea textArea = new TextArea(result.getDetails());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(500);
        textArea.setPrefHeight(300);
        
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }
}

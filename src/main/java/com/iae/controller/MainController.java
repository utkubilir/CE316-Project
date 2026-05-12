package com.iae.controller;

import com.iae.model.StudentResult;
import com.iae.model.TestStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

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

    @FXML
    public void initialize() {
        languageCombo.setItems(FXCollections.observableArrayList("C", "C++", "Java", "Python"));
        languageCombo.getSelectionModel().select("C");

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

    @FXML private void onNewProject() { info("New Project", "Stub — will be wired to ProjectService."); }
    @FXML private void onOpenProject() { info("Open Project", "Stub — will load a .iae project JSON."); }
    @FXML private void onSaveProject() { info("Save Project", "Stub — will save current state to JSON."); }
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
        info("Run Tests", "Stub — will extract ZIPs, compile, run, compare, and populate the Test Results table.");
    }

    private void info(String title, String body) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, body, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(title);
        a.showAndWait();
    }
}

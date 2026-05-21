package com.iae.service;

import com.iae.model.Configuration;
import com.iae.model.StudentResult;

import com.iae.model.Project;
import com.iae.repository.ProjectRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class ProjectService {

    private final FileManager fileManager =
            new FileManager();

    private final ExecutionEngine executionEngine =
            new ExecutionEngine();

    private final ProjectRepository projectRepository = new ProjectRepository();
    
    private Project currentProject;

    public void saveProject(Project project) {
        projectRepository.save(project);
    }

    public Project loadProject(String name) {
        return projectRepository.load(name);
    }

    public void deleteProject(String name) {
        projectRepository.delete(name);
        if (currentProject != null && name != null && name.equals(currentProject.getName())) {
            currentProject = null;
        }
    }

    public List<String> getAllProjectNames() {
        return projectRepository.getAllProjectNames();
    }

    public Project getCurrentProject() {
        return currentProject;
    }

    public void setCurrentProject(Project currentProject) {
        this.currentProject = currentProject;
    }

    public interface EvaluationProgress {
        void onSubmissionStarted(String studentId, int completed, int total);
        void onSubmissionFinished(StudentResult result, int completed, int total);
    }

    public List<StudentResult> runEvaluation(
            File submissionsFolder,
            Configuration configuration,
            String expectedOutput
    ) {
        return runEvaluation(submissionsFolder, configuration, expectedOutput, null, () -> false);
    }

    public List<StudentResult> runEvaluation(
            File submissionsFolder,
            Configuration configuration,
            String expectedOutput,
            EvaluationProgress progress,
            BooleanSupplier cancelled
    ) {

        List<StudentResult> results =
                new ArrayList<>();

        List<File> zipFiles =
                fileManager.discoverZipFiles(submissionsFolder);

        File workingDirectory =
                new File("working_directory");

        if (!workingDirectory.exists()) {
            workingDirectory.mkdirs();
        }

        int total = zipFiles.size();
        int completed = 0;

        for (File zipFile : zipFiles) {
            if (cancelled != null && cancelled.getAsBoolean()) {
                break;
            }

            String zipStudentId = studentIdFromZip(zipFile);
            if (progress != null) {
                progress.onSubmissionStarted(zipStudentId, completed, total);
            }

            try {

                File studentFolder =
                        fileManager.extractZip(
                                zipFile,
                                workingDirectory
                        );

                String studentId =
                        studentFolder.getName();

                File sourceFile =
                        fileManager.findSourceFile(
                                studentFolder,
                                configuration.getSourceFileName()
                        );

                if (sourceFile == null) {

                    StudentResult missingSource = new StudentResult(
                            studentId,
                            com.iae.model.TestStatus.MISSING_SOURCE,
                            "Source file not found.");
                    results.add(missingSource);
                    completed++;
                    if (progress != null) {
                        progress.onSubmissionFinished(missingSource, completed, total);
                    }

                    continue;
                }

                StudentResult result =
                        executionEngine.evaluateSubmission(
                                studentId,
                                sourceFile.getParentFile(),
                                configuration.getCompileCommand(),
                                configuration.getRunCommand(),
                                expectedOutput
                        );

                results.add(result);
                completed++;
                if (progress != null) {
                    progress.onSubmissionFinished(result, completed, total);
                }

            } catch (Exception e) {

                StudentResult extractionError = new StudentResult(
                        zipFile.getName(),
                        com.iae.model.TestStatus.EXTRACTION_ERROR,
                        e.getMessage());
                results.add(extractionError);
                completed++;
                if (progress != null) {
                    progress.onSubmissionFinished(extractionError, completed, total);
                }
            }
        }

        return results;
    }

    private String studentIdFromZip(File zipFile) {
        String fileName = zipFile.getName();
        int dot = fileName.lastIndexOf(".");
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}

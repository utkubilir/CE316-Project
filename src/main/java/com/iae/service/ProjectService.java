package com.iae.service;

import com.iae.model.Configuration;
import com.iae.model.StudentResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectService {

    private final FileManager fileManager =
            new FileManager();

    private final ExecutionEngine executionEngine =
            new ExecutionEngine();

    public List<StudentResult> runEvaluation(
            File submissionsFolder,
            Configuration configuration,
            String expectedOutput
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

        for (File zipFile : zipFiles) {

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

                    results.add(
                            new StudentResult(
                                    studentId,
                                    com.iae.model.TestStatus.MISSING_SOURCE,
                                    "Source file not found."
                            )
                    );

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

            } catch (Exception e) {

                results.add(
                        new StudentResult(
                                zipFile.getName(),
                                com.iae.model.TestStatus.EXTRACTION_ERROR,
                                e.getMessage()
                        )
                );
            }
        }

        return results;
    }
}

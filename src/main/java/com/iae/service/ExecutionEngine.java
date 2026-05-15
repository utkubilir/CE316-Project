package com.iae.service;

import com.iae.model.StudentResult;
import com.iae.model.TestStatus;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ExecutionEngine {

    public String compile(File workingDirectory,
                          String compileCommand) {

        try {

            ProcessBuilder pb = new ProcessBuilder(
                    compileCommand.split(" ")
            );

            pb.directory(workingDirectory);

            pb.redirectErrorStream(true);

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            StringBuilder output = new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return "SUCCESS";
            }

            return output.toString();

        } catch (Exception e) {

            return e.getMessage();
        }
    }

    public String runProgram(File workingDirectory,
                             String runCommand) {

        try {

            ProcessBuilder pb = new ProcessBuilder(
                    runCommand.split(" ")
            );

            pb.directory(workingDirectory);

            pb.redirectErrorStream(true);

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            StringBuilder output = new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            boolean finished =
                    process.waitFor(5, TimeUnit.SECONDS);

            if (!finished) {

                process.destroyForcibly();

                return "TIMEOUT";
            }

            int exitCode = process.exitValue();

            if (exitCode != 0) {

                return "RUNTIME_ERROR\n"
                        + output;
            }

            return output.toString();

        } catch (Exception e) {

            return "RUNTIME_ERROR\n"
                    + e.getMessage();
        }
    }

    public boolean compareOutput(String actualOutput,
                                 String expectedOutput) {

        String normalizedActual =
                actualOutput.trim().replace("\r\n", "\n");

        String normalizedExpected =
                expectedOutput.trim().replace("\r\n", "\n");

        return normalizedActual.equals(normalizedExpected);
    }

    public StudentResult evaluateSubmission(
            String studentId,
            File workingDirectory,
            String compileCommand,
            String runCommand,
            String expectedOutput
    ) {

        String compileResult =
                compile(workingDirectory, compileCommand);

        if (!compileResult.equals("SUCCESS")) {

            return new StudentResult(
                    studentId,
                    TestStatus.COMPILATION_ERROR,
                    compileResult
            );
        }

        String actualOutput =
                runProgram(workingDirectory, runCommand);

        if (actualOutput.equals("TIMEOUT")) {

            return new StudentResult(
                    studentId,
                    TestStatus.TIMEOUT,
                    "Program execution timed out."
            );
        }

        if (actualOutput.startsWith("RUNTIME_ERROR")) {

            return new StudentResult(
                    studentId,
                    TestStatus.RUNTIME_ERROR,
                    actualOutput
            );
        }

        boolean matches =
                compareOutput(actualOutput, expectedOutput);

        if (matches) {

            return new StudentResult(
                    studentId,
                    TestStatus.PASSED,
                    ""
            );
        }

        return new StudentResult(
                studentId,
                TestStatus.OUTPUT_MISMATCH,
                "Expected:\n"
                        + expectedOutput
                        + "\n\nFound:\n"
                        + actualOutput
        );
    }
}
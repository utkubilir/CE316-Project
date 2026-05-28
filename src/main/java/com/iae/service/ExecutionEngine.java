package com.iae.service;

import com.iae.model.StudentResult;
import com.iae.model.TestStatus;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs the configuration's compile / run commands against a student's
 * working directory and compares the program's output to the expected
 * output (Requirements #7 and #8).
 *
 * <p>Every {@link StudentResult} produced here also carries a 0-100 grade
 * derived from its {@link TestStatus} (tiered scoring, see
 * {@link TestStatus#score()}): a clean pass scores 100, an output mismatch
 * earns partial credit, and error / missing-source outcomes score lower.
 */
public class ExecutionEngine {

    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("\"([^\"]*)\"|(\\S+)");

    public String compile(File workingDirectory, String compileCommand) {

        if (compileCommand == null || compileCommand.isBlank()) {
            return "SUCCESS";
        }

        try {
            List<String> tokens = resolveTokens(workingDirectory, tokenize(compileCommand));

            ProcessBuilder pb = new ProcessBuilder(tokens);
            pb.directory(workingDirectory);
            pb.redirectErrorStream(true);

            File tempOut = File.createTempFile("compile_out", ".txt");
            pb.redirectOutput(tempOut);

            Process process = pb.start();
            int exitCode = process.waitFor();
            String output = Files.readString(tempOut.toPath());
            tempOut.delete();

            if (exitCode == 0) {
                return "SUCCESS";
            }

            return output.isBlank() ? ("Compile failed with exit code " + exitCode) : output;

        } catch (Exception e) {
            return "Compile failed: " + e.getMessage();
        }
    }

    public String runProgram(File workingDirectory, String runCommand) {
        return runProgram(workingDirectory, runCommand, List.of());
    }

    public String runProgram(File workingDirectory, String runCommand, List<String> runArgs) {

        if (runCommand == null || runCommand.isBlank()) {
            return "RUNTIME_ERROR\nRun command is empty.";
        }

        try {
            List<String> tokens = resolveTokens(workingDirectory, tokenize(runCommand));
            if (runArgs != null) {
                for (String arg : runArgs) {
                    if (arg != null) {
                        tokens.add(arg);
                    }
                }
            }

            ProcessBuilder pb = new ProcessBuilder(tokens);
            pb.directory(workingDirectory);
            pb.redirectErrorStream(true);

            File tempOut = File.createTempFile("run_out", ".txt");
            pb.redirectOutput(tempOut);

            Process process = pb.start();

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);

            if (!finished) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                process.waitFor(); // Wait for it to actually die
                tempOut.delete();
                return "TIMEOUT";
            }

            int exitCode = process.exitValue();
            String output = Files.readString(tempOut.toPath());
            tempOut.delete();

            if (exitCode != 0) {
                return "RUNTIME_ERROR\nExit code " + exitCode + "\n" + output;
            }

            return output;

        } catch (Exception e) {
            return "RUNTIME_ERROR\n" + e.getMessage();
        }
    }

    public boolean compareOutput(String actualOutput, String expectedOutput) {
        return normalizeOutput(actualOutput).equals(normalizeOutput(expectedOutput));
    }

    public StudentResult evaluateSubmission(
            String studentId,
            File workingDirectory,
            String compileCommand,
            String runCommand,
            String expectedOutput
    ) {
        return evaluateSubmission(studentId, workingDirectory, compileCommand, runCommand, List.of(), expectedOutput);
    }

    public StudentResult evaluateSubmission(
            String studentId,
            File workingDirectory,
            String compileCommand,
            String runCommand,
            List<String> runArgs,
            String expectedOutput
    ) {

        String compileResult = compile(workingDirectory, compileCommand);

        if (!compileResult.equals("SUCCESS")) {
            return new StudentResult(studentId, TestStatus.COMPILATION_ERROR, compileResult);
        }

        String actualOutput = runProgram(workingDirectory, runCommand, runArgs);

        if (actualOutput.equals("TIMEOUT")) {
            return new StudentResult(studentId, TestStatus.TIMEOUT, "Program execution timed out.");
        }

        if (actualOutput.startsWith("RUNTIME_ERROR")) {
            return new StudentResult(studentId, TestStatus.RUNTIME_ERROR, actualOutput);
        }

        if (compareOutput(actualOutput, expectedOutput)) {
            return new StudentResult(studentId, TestStatus.PASSED, "");
        }

        String mismatchDetails = findMismatchDetails(actualOutput, expectedOutput);
        return new StudentResult(studentId, TestStatus.OUTPUT_MISMATCH, mismatchDetails);
    }

    // ----- helpers ---------------------------------------------------------

    /** Splits a command string respecting double-quoted arguments. */
    public static List<String> tokenize(String cmd) {
        List<String> tokens = new ArrayList<>();
        if (cmd == null) return tokens;
        Matcher m = TOKEN_PATTERN.matcher(cmd);
        while (m.find()) {
            tokens.add(m.group(1) != null ? m.group(1) : m.group(2));
        }
        return tokens;
    }

    /**
     * Resolves the first token to an absolute path if it refers to a file
     * inside the working directory. ProcessBuilder on Windows does NOT
     * search the working directory for executables, so a command like
     * "main.exe" fails unless we expand it to its absolute path.
     * Also tries appending ".exe" if the raw name is not found.
     */
    static List<String> resolveTokens(File workingDirectory, List<String> tokens) {
        if (tokens.isEmpty() || workingDirectory == null) {
            return tokens;
        }

        String first = tokens.get(0);
        File asIs = new File(workingDirectory, first);
        if (asIs.isFile()) {
            tokens.set(0, asIs.getAbsolutePath());
            return tokens;
        }

        File withExe = new File(workingDirectory, first + ".exe");
        if (withExe.isFile()) {
            tokens.set(0, withExe.getAbsolutePath());
            return tokens;
        }

        // Tolerate "./main" / ".\main" style invocations on Windows.
        if (first.startsWith("./") || first.startsWith(".\\")) {
            String stripped = first.substring(2);
            File s = new File(workingDirectory, stripped);
            if (s.isFile()) {
                tokens.set(0, s.getAbsolutePath());
            } else {
                File se = new File(workingDirectory, stripped + ".exe");
                if (se.isFile()) tokens.set(0, se.getAbsolutePath());
            }
        }

        return tokens;
    }



    private String normalizeOutput(String text) {
        if (text == null) return "";
        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim()
                .replaceAll("[ \t]+", " ")
                .replaceAll("\n+", "\n");
    }

    private String findMismatchDetails(String actualOutput, String expectedOutput) {
        String[] actualLines = normalizeOutput(actualOutput).split("\n");
        String[] expectedLines = normalizeOutput(expectedOutput).split("\n");
        int minLength = Math.min(actualLines.length, expectedLines.length);

        for (int i = 0; i < minLength; i++) {
            if (!actualLines[i].equals(expectedLines[i])) {
                return "Mismatch at line " + (i + 1)
                        + "\nExpected: " + expectedLines[i]
                        + "\nFound:    " + actualLines[i];
            }
        }

        if (actualLines.length != expectedLines.length) {
            return "Line count mismatch.\n"
                    + "Expected lines: " + expectedLines.length
                    + "\nFound lines:    " + actualLines.length;
        }

        return "Output mismatch (no specific line difference detected).";
    }
}

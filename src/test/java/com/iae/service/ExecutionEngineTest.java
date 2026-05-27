package com.iae.service;

import com.iae.model.StudentResult;
import com.iae.model.TestStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionEngineTest {

    private final ExecutionEngine engine = new ExecutionEngine();

    @Test
    void tokenizeRespectsDoubleQuotedArguments() {
        List<String> tokens = ExecutionEngine.tokenize("java Main \"hello world\" bye");
        assertEquals(List.of("java", "Main", "hello world", "bye"), tokens);
    }

    @Test
    void tokenizeReturnsEmptyListForNullOrBlank() {
        assertTrue(ExecutionEngine.tokenize(null).isEmpty());
        assertTrue(ExecutionEngine.tokenize("   ").isEmpty());
    }

    @Test
    void resolveTokensExpandsWorkingDirectoryExecutable(@TempDir Path tempDir) throws Exception {
        File exe = tempDir.resolve("main").toFile();
        Files.writeString(exe.toPath(), "#!/bin/sh\nexit 0\n");
        exe.setExecutable(true);

        List<String> resolved = ExecutionEngine.resolveTokens(
                tempDir.toFile(), new java.util.ArrayList<>(List.of("main")));
        assertEquals(exe.getAbsolutePath(), resolved.get(0));
    }

    @Test
    void resolveTokensAppendsExeWhenMissing(@TempDir Path tempDir) throws Exception {
        File exe = tempDir.resolve("main.exe").toFile();
        Files.writeString(exe.toPath(), "");

        List<String> resolved = ExecutionEngine.resolveTokens(
                tempDir.toFile(), new java.util.ArrayList<>(List.of("main")));
        assertEquals(exe.getAbsolutePath(), resolved.get(0));
    }

    @Test
    void compareOutputNormalizesLineEndings() {
        assertTrue(engine.compareOutput("hello\r\nworld\n", "hello\nworld"));
    }

    @Test
    void compareOutputCollapsesWhitespace() {
        assertTrue(engine.compareOutput("hello   world", "hello world"));
        assertFalse(engine.compareOutput("hello", "world"));
    }

    @Test
    void evaluateSubmissionFlowsToRuntimeErrorWhenRunCommandEmpty() {
        StudentResult result = engine.evaluateSubmission(
                "S001",
                new File(System.getProperty("java.io.tmpdir")),
                "",
                "",
                "");
        assertEquals(TestStatus.RUNTIME_ERROR, result.getStatus());
    }

    @Test
    void evaluateSubmissionPassesWhenJavaProgramEchoesArgs(@TempDir Path tempDir) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                isExecutableOnPath("javac"), "javac must be available on PATH for this test");
        org.junit.jupiter.api.Assumptions.assumeTrue(
                isExecutableOnPath("java"), "java must be available on PATH for this test");

        Files.writeString(tempDir.resolve("Main.java"),
                "public class Main {" +
                "  public static void main(String[] a){" +
                "    StringBuilder b=new StringBuilder();" +
                "    for(int i=0;i<a.length;i++){if(i>0)b.append(' ');b.append(a[i]);}" +
                "    System.out.println(b);" +
                "  }" +
                "}\n");

        StudentResult result = engine.evaluateSubmission(
                "S002",
                tempDir.toFile(),
                "javac Main.java",
                "java Main",
                List.of("hello", "world"),
                "hello world");

        assertEquals(TestStatus.PASSED, result.getStatus(),
                () -> "Unexpected status. Details: " + result.getDetails());
    }

    @Test
    void evaluateSubmissionReportsMismatchWhenOutputDiffers(@TempDir Path tempDir) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                isExecutableOnPath("javac"), "javac must be available on PATH for this test");
        org.junit.jupiter.api.Assumptions.assumeTrue(
                isExecutableOnPath("java"), "java must be available on PATH for this test");

        Files.writeString(tempDir.resolve("Main.java"),
                "public class Main { public static void main(String[] a) { System.out.println(\"actual\"); } }\n");

        StudentResult result = engine.evaluateSubmission(
                "S003",
                tempDir.toFile(),
                "javac Main.java",
                "java Main",
                "expected");

        assertEquals(TestStatus.OUTPUT_MISMATCH, result.getStatus());
        assertTrue(result.getDetails().contains("Mismatch at line 1"));
    }

    private static boolean isExecutableOnPath(String name) {
        String path = System.getenv("PATH");
        if (path == null) return false;
        for (String dir : path.split(File.pathSeparator)) {
            File f = new File(dir, name);
            if (f.canExecute()) return true;
            File withExe = new File(dir, name + ".exe");
            if (withExe.canExecute()) return true;
        }
        return false;
    }
}

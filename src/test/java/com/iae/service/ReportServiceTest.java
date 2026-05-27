package com.iae.service;

import com.iae.model.Configuration;
import com.iae.model.Project;
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

class ReportServiceTest {

    private final ReportService reportService = new ReportService();

    @Test
    void exportReportWritesHtmlWithSummaryCounts(@TempDir Path tempDir) throws Exception {
        Project p = makeProject();
        File dest = tempDir.resolve("report.html").toFile();

        reportService.exportReport(p, "expected", dest);

        String html = Files.readString(dest.toPath());
        assertTrue(html.contains("<html"));
        assertTrue(html.contains("Round Trip Demo"));
        assertTrue(html.contains("Passed"));
        assertTrue(html.contains("Compile Error"));
        assertTrue(html.contains("alpha beta"), "Run args should appear in the configuration block");
    }

    @Test
    void htmlEscapesDangerousCharactersInDetails(@TempDir Path tempDir) throws Exception {
        Project p = makeProject();
        p.getResults().clear();
        p.getResults().add(new StudentResult("S1", TestStatus.RUNTIME_ERROR, "<script>alert('x')</script>"));

        File dest = tempDir.resolve("report.html").toFile();
        reportService.exportReport(p, null, dest);
        String html = Files.readString(dest.toPath());

        assertTrue(html.contains("&lt;script&gt;"), "Tags must be escaped");
        assertFalse(html.contains("<script>alert"), "Raw script tag must not appear");
    }

    @Test
    void buildHtmlHandlesProjectWithNoResults() {
        Project p = new Project();
        p.setName("Empty");
        p.setResults(List.of());
        String html = reportService.buildHtml(p, null);
        assertTrue(html.contains("No student submissions evaluated."));
    }

    @Test
    void escapeHandlesAllSpecialCharacters() {
        assertEquals("&amp;&lt;&gt;&quot;&#39;", ReportService.escape("&<>\"'"));
    }

    private static Project makeProject() {
        Project p = new Project();
        p.setName("Round Trip Demo");
        p.setSubmissionFolder("/tmp/zips");
        p.setExpectedOutputPath("/tmp/expected.txt");
        Configuration cfg = new Configuration("Java cfg", "Java");
        cfg.setSourceFileName("Main.java");
        cfg.setCompileCommand("javac Main.java");
        cfg.setRunCommand("java Main");
        cfg.setRunArgs(List.of("alpha", "beta"));
        p.setConfiguration(cfg);
        p.getResults().add(new StudentResult("S1", TestStatus.PASSED, ""));
        p.getResults().add(new StudentResult("S2", TestStatus.COMPILATION_ERROR, "missing semicolon"));
        return p;
    }
}

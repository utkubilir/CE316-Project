package com.iae.repository;

import com.iae.model.Configuration;
import com.iae.model.Project;
import com.iae.model.StudentResult;
import com.iae.model.TestStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectRepositoryRoundTripTest {

    @TempDir
    Path tempDir;

    private String originalDatabaseUrl;
    private ProjectRepository repo;

    @BeforeEach
    void setUp() {
        originalDatabaseUrl = System.getProperty(DatabaseHelper.DATABASE_URL_PROPERTY);
        System.setProperty(DatabaseHelper.DATABASE_URL_PROPERTY,
                "jdbc:sqlite:" + tempDir.resolve("project.db"));
        repo = new ProjectRepository();
    }

    @AfterEach
    void tearDown() {
        if (originalDatabaseUrl == null) {
            System.clearProperty(DatabaseHelper.DATABASE_URL_PROPERTY);
        } else {
            System.setProperty(DatabaseHelper.DATABASE_URL_PROPERTY, originalDatabaseUrl);
        }
    }

    @Test
    void roundTripPreservesConfigurationResultsAndRunArgs() {
        Project p = makeProject("Demo");
        p.getConfiguration().setRunArgs(List.of("alpha", "beta"));
        p.getResults().add(new StudentResult("S1", TestStatus.PASSED, ""));
        p.getResults().add(new StudentResult("S2", TestStatus.COMPILATION_ERROR, "syntax error"));
        p.getResults().add(new StudentResult("S3", TestStatus.OUTPUT_MISMATCH, "Mismatch at line 1"));

        repo.save(p);

        Project loaded = repo.load("Demo");
        assertNotNull(loaded);
        assertEquals("Demo", loaded.getName());
        assertEquals(p.getSubmissionFolder(), loaded.getSubmissionFolder());
        assertEquals(p.getConfiguration().getName(), loaded.getConfiguration().getName());
        assertEquals(p.getConfiguration().getRunArgs(), loaded.getConfiguration().getRunArgs());
        assertEquals(3, loaded.getResults().size());
        assertEquals(TestStatus.PASSED, loaded.getResults().get(0).getStatus());
    }

    @Test
    void savingTwiceReplacesResultsInsteadOfDuplicating() {
        Project p = makeProject("Replay");
        p.getResults().add(new StudentResult("S1", TestStatus.PASSED, ""));
        repo.save(p);

        p.getResults().clear();
        p.getResults().add(new StudentResult("S2", TestStatus.TIMEOUT, "10s"));
        p.getResults().add(new StudentResult("S3", TestStatus.RUNTIME_ERROR, "exit 1"));
        repo.save(p);

        Project loaded = repo.load("Replay");
        assertEquals(2, loaded.getResults().size());
        assertTrue(loaded.getResults().stream().noneMatch(r -> r.getStudentId().equals("S1")));
    }

    @Test
    void deleteCascadesConfigurationAndResults() {
        Project p = makeProject("Doomed");
        p.getResults().add(new StudentResult("S1", TestStatus.PASSED, ""));
        repo.save(p);

        repo.delete("Doomed");

        assertNull(repo.load("Doomed"));
        assertTrue(repo.getAllProjectNames().stream().noneMatch("Doomed"::equals));
    }

    private static Project makeProject(String name) {
        Project p = new Project();
        p.setName(name);
        p.setSubmissionFolder("/tmp/" + name);
        p.setExpectedOutputPath("/tmp/" + name + "/expected.txt");
        Configuration cfg = new Configuration(name + " Config", "Java");
        cfg.setSourceFileName("Main.java");
        cfg.setCompileCommand("javac Main.java");
        cfg.setRunCommand("java Main");
        cfg.setCompiled(true);
        p.setConfiguration(cfg);
        return p;
    }
}

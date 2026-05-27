package com.iae.repository;

import com.iae.model.Configuration;
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

class ConfigurationRepositoryRoundTripTest {

    @TempDir
    Path tempDir;

    private String originalDatabaseUrl;
    private ConfigurationRepository repo;

    @BeforeEach
    void setUp() {
        originalDatabaseUrl = System.getProperty(DatabaseHelper.DATABASE_URL_PROPERTY);
        System.setProperty(DatabaseHelper.DATABASE_URL_PROPERTY,
                "jdbc:sqlite:" + tempDir.resolve("rt.db"));
        repo = new ConfigurationRepository();
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
    void roundTripPreservesAllFieldsIncludingRunArgs() {
        Configuration cfg = new Configuration();
        cfg.setName("Round Trip");
        cfg.setLanguage("Java");
        cfg.setSourceFileName("Main.java");
        cfg.setCompileCommand("javac Main.java");
        cfg.setRunCommand("java Main");
        cfg.setRunArgs(List.of("foo", "bar baz"));
        cfg.setCompiled(true);

        repo.save(cfg);

        Configuration loaded = repo.findByName("Round Trip");
        assertNotNull(loaded);
        assertEquals(cfg.getName(), loaded.getName());
        assertEquals(cfg.getLanguage(), loaded.getLanguage());
        assertEquals(cfg.getSourceFileName(), loaded.getSourceFileName());
        assertEquals(cfg.getCompileCommand(), loaded.getCompileCommand());
        assertEquals(cfg.getRunCommand(), loaded.getRunCommand());
        assertEquals(cfg.getRunArgs(), loaded.getRunArgs());
        assertEquals(cfg.isCompiled(), loaded.isCompiled());
    }

    @Test
    void findAllNamesReturnsNamesAlphabetically() {
        repo.save(makeConfig("Zeta", "Java"));
        repo.save(makeConfig("Alpha", "C"));
        repo.save(makeConfig("Mu", "Python"));

        List<String> names = repo.findAllNames();
        assertEquals(List.of("Alpha", "Mu", "Zeta"), names);
    }

    @Test
    void deleteRemovesConfiguration() {
        repo.save(makeConfig("Deletable", "Java"));
        assertNotNull(repo.findByName("Deletable"));
        repo.delete("Deletable");
        assertNull(repo.findByName("Deletable"));
    }

    @Test
    void saveTwiceUpdatesExistingRow() {
        Configuration cfg = makeConfig("Mutable", "C");
        cfg.setRunCommand("first");
        repo.save(cfg);
        cfg.setRunCommand("second");
        repo.save(cfg);

        Configuration loaded = repo.findByName("Mutable");
        assertEquals("second", loaded.getRunCommand());
        assertTrue(repo.findAllNames().stream().filter("Mutable"::equals).count() == 1);
    }

    private static Configuration makeConfig(String name, String language) {
        Configuration cfg = new Configuration(name, language);
        cfg.setSourceFileName("Main." + language.toLowerCase());
        cfg.setRunCommand("./run");
        cfg.setCompiled(true);
        return cfg;
    }
}

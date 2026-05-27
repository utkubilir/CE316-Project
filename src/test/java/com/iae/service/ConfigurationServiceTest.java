package com.iae.service;

import com.iae.model.Configuration;
import com.iae.repository.DatabaseHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationServiceTest {

    @TempDir
    Path tempDir;

    private String originalDatabaseUrl;
    private ConfigurationService service;

    @BeforeEach
    void setUp() {
        originalDatabaseUrl = System.getProperty(DatabaseHelper.DATABASE_URL_PROPERTY);
        System.setProperty(DatabaseHelper.DATABASE_URL_PROPERTY,
                "jdbc:sqlite:" + tempDir.resolve("config.db"));
        service = new ConfigurationService();
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
    void applyLanguageDefaultsPopulatesAllFourBuiltins() {
        for (String language : List.of("C", "C++", "Java", "Python")) {
            Configuration cfg = service.createConfiguration(language + " Config", language);
            assertNotNull(cfg.getSourceFileName(), language + " missing source file");
            assertNotNull(cfg.getRunCommand(), language + " missing run command");
        }
    }

    @Test
    void seedDefaultsIfEmptyIsIdempotent() {
        service.seedDefaultsIfEmpty();
        int firstCount = service.listConfigurationNames().size();
        service.seedDefaultsIfEmpty();
        int secondCount = service.listConfigurationNames().size();
        assertEquals(firstCount, secondCount);
        assertTrue(firstCount >= 4);
    }

    @Test
    void exportImportSingleRoundTrip() throws Exception {
        Configuration cfg = service.createConfiguration("Round Trip", "Java");
        cfg.setRunArgs(List.of("alpha", "beta gamma"));

        File dest = tempDir.resolve("single.json").toFile();
        service.exportConfiguration(cfg, dest);

        Configuration imported = service.importConfiguration(dest);
        assertEquals(cfg.getName(), imported.getName());
        assertEquals(cfg.getLanguage(), imported.getLanguage());
        assertEquals(cfg.getCompileCommand(), imported.getCompileCommand());
        assertEquals(cfg.getRunCommand(), imported.getRunCommand());
        assertEquals(cfg.getRunArgs(), imported.getRunArgs());
    }

    @Test
    void exportImportBulkRoundTrip() throws Exception {
        List<Configuration> configs = List.of(
                service.createConfiguration("C cfg", "C"),
                service.createConfiguration("Java cfg", "Java"),
                service.createConfiguration("Py cfg", "Python"));

        File dest = tempDir.resolve("bulk.json").toFile();
        service.exportConfigurations(configs, dest);

        List<Configuration> imported = service.importConfigurations(dest);
        assertEquals(3, imported.size());
        assertEquals("C cfg", imported.get(0).getName());
        assertEquals("Python", imported.get(2).getLanguage());
    }

    @Test
    void importConfigurationsAcceptsSingleObjectFile() throws Exception {
        Configuration cfg = service.createConfiguration("Lone", "C");
        File dest = tempDir.resolve("lone.json").toFile();
        service.exportConfiguration(cfg, dest);

        List<Configuration> imported = service.importConfigurations(dest);
        assertEquals(1, imported.size());
        assertEquals("Lone", imported.get(0).getName());
    }
}

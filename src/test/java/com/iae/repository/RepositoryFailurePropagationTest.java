package com.iae.repository;

import com.iae.model.Configuration;
import com.iae.model.Project;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositoryFailurePropagationTest {

    @TempDir
    Path tempDirectory;

    private String originalDatabaseUrl;
    private ProjectRepository projectRepository;
    private ConfigurationRepository configurationRepository;

    @BeforeEach
    void initializeWritableDatabase() {
        originalDatabaseUrl = System.getProperty(DatabaseHelper.DATABASE_URL_PROPERTY);
        System.setProperty(DatabaseHelper.DATABASE_URL_PROPERTY,
                "jdbc:sqlite:" + tempDirectory.resolve("working.db"));
        projectRepository = new ProjectRepository();
        configurationRepository = new ConfigurationRepository();
    }

    @AfterEach
    void restoreDatabaseUrl() {
        if (originalDatabaseUrl == null) {
            System.clearProperty(DatabaseHelper.DATABASE_URL_PROPERTY);
        } else {
            System.setProperty(DatabaseHelper.DATABASE_URL_PROPERTY, originalDatabaseUrl);
        }
    }

    @Test
    void projectSaveFailureIsPropagated() {
        pointAtUnavailableDatabase();
        Project project = new Project();
        project.setName("Failed save");

        assertThrows(PersistenceException.class, () -> projectRepository.save(project));
    }

    @Test
    void projectDeleteFailureIsPropagated() {
        pointAtUnavailableDatabase();

        assertThrows(PersistenceException.class, () -> projectRepository.delete("Unavailable"));
    }

    @Test
    void configurationSaveFailureIsPropagated() {
        pointAtUnavailableDatabase();
        Configuration configuration = new Configuration("Failed save", "Java");

        assertThrows(PersistenceException.class, () -> configurationRepository.save(configuration));
    }

    @Test
    void configurationDeleteFailureIsPropagated() {
        pointAtUnavailableDatabase();

        assertThrows(PersistenceException.class, () -> configurationRepository.delete("Unavailable"));
    }

    private void pointAtUnavailableDatabase() {
        System.setProperty(DatabaseHelper.DATABASE_URL_PROPERTY,
                "jdbc:sqlite:" + tempDirectory.resolve("missing-directory").resolve("unavailable.db"));
    }
}

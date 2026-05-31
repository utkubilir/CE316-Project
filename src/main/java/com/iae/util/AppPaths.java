package com.iae.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central resolver for the per-user directory where the application keeps its
 * runtime data (the SQLite database, the temporary extraction working
 * directory, exported reports, ...).
 *
 * <p>
 * When the app is installed under a read-only location such as
 * {@code C:\Program Files}, writing next to the executable fails. All runtime
 * read/write therefore goes through here, which points at a writable per-user
 * folder:
 * <ul>
 * <li>Windows: {@code %APPDATA%\CE316-IAE}</li>
 * <li>Other / {@code APPDATA} unset (e.g. tests): {@code ~/.ce316-iae}</li>
 * </ul>
 */
public final class AppPaths {

    private static final String APP_FOLDER = "CE316-IAE";

    private AppPaths() {
    }

    /** The base data directory, created on demand. */
    public static Path dataDir() {
        String appData = System.getenv("APPDATA");
        Path base;
        if (appData != null && !appData.isBlank()) {
            base = Paths.get(appData, APP_FOLDER);
        } else {
            base = Paths.get(System.getProperty("user.home"), ".ce316-iae");
        }
        return ensureDir(base);
    }

    /** Absolute path of the SQLite database file (parent directory is created). */
    public static Path databaseFile() {
        return dataDir().resolve("iae_projects.db");
    }

    /** The working directory used for extracting and running submissions. */
    public static File workingDir() {
        return ensureDir(dataDir().resolve("working_directory")).toFile();
    }

    private static Path ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create application directory: " + dir, e);
        }
        return dir;
    }
}

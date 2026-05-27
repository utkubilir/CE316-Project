package com.iae.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHelper {

    private static final String DEFAULT_URL = "jdbc:sqlite:iae_projects.db";
    static final String DATABASE_URL_PROPERTY = "iae.database.url";

    public static Connection getConnection() throws SQLException {
        String url = System.getProperty(DATABASE_URL_PROPERTY, DEFAULT_URL);
        return DriverManager.getConnection(url);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create projects table
            String createProjectsTable = "CREATE TABLE IF NOT EXISTS projects (" +
                    "name TEXT PRIMARY KEY, " +
                    "submission_folder TEXT, " +
                    "report_path TEXT, " +
                    "expected_output_path TEXT" +
                    ");";
            stmt.execute(createProjectsTable);

            // Attempt to add expected_output_path column to existing projects table (migration)
            try {
                stmt.execute("ALTER TABLE projects ADD COLUMN expected_output_path TEXT;");
            } catch (SQLException ignore) {
                // Ignore if the column already exists
            }

            // Create configurations table
            String createConfigsTable = "CREATE TABLE IF NOT EXISTS configurations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "project_name TEXT, " +
                    "name TEXT, " +
                    "language TEXT, " +
                    "source_file_name TEXT, " +
                    "compile_command TEXT, " +
                    "run_command TEXT, " +
                    "compiled INTEGER, " +
                    "FOREIGN KEY(project_name) REFERENCES projects(name) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createConfigsTable);

            // Create results table
            String createResultsTable = "CREATE TABLE IF NOT EXISTS results (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "project_name TEXT, " +
                    "student_id TEXT, " +
                    "status TEXT, " +
                    "details TEXT, " +
                    "FOREIGN KEY(project_name) REFERENCES projects(name) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createResultsTable);

            // Standalone, reusable configurations (Requirement #4).
            // Independent from project-scoped configurations so they survive
            // across projects and can be picked when creating a new project.
            String createSavedConfigsTable = "CREATE TABLE IF NOT EXISTS saved_configurations (" +
                    "name TEXT PRIMARY KEY, " +
                    "language TEXT, " +
                    "source_file_name TEXT, " +
                    "compile_command TEXT, " +
                    "run_command TEXT, " +
                    "compiled INTEGER" +
                    ");";
            stmt.execute(createSavedConfigsTable);

        } catch (SQLException e) {
            throw new PersistenceException("Could not initialize the local database.", e);
        }
    }
}

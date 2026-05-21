package com.iae.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHelper {

    private static final String URL = "jdbc:sqlite:iae_projects.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create projects table
            String createProjectsTable = "CREATE TABLE IF NOT EXISTS projects (" +
                    "name TEXT PRIMARY KEY, " +
                    "submission_folder TEXT, " +
                    "report_path TEXT" +
                    ");";
            stmt.execute(createProjectsTable);

            // Create configurations table
            String createConfigsTable = "CREATE TABLE IF NOT EXISTS configurations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "project_name TEXT, " +
                    "name TEXT, " +
                    "language TEXT, " +
                    "source_file_name TEXT, " +
                    "compile_command TEXT, " +
                    "run_command TEXT, " +
                    "expected_output_path TEXT, " +
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
                    "expected_output_path TEXT, " +
                    "compiled INTEGER" +
                    ");";
            stmt.execute(createSavedConfigsTable);

        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }
}

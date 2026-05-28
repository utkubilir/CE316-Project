package com.iae.repository;

import com.iae.model.Configuration;
import com.iae.model.Project;
import com.iae.model.StudentResult;
import com.iae.model.TestStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProjectRepository {

    public ProjectRepository() {
        DatabaseHelper.initializeDatabase();
    }

    public void save(Project project) {
        if (project.getName() == null || project.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be empty");
        }

        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Insert or update the project
            String upsertProject = "INSERT INTO projects (name, submission_folder, report_path, expected_output_path) " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT(name) DO UPDATE SET " +
                    "submission_folder=excluded.submission_folder, " +
                    "report_path=excluded.report_path, " +
                    "expected_output_path=excluded.expected_output_path;";
            try (PreparedStatement pstmt = conn.prepareStatement(upsertProject)) {
                pstmt.setString(1, project.getName());
                pstmt.setString(2, project.getSubmissionFolder());
                pstmt.setString(3, project.getReportPath());
                pstmt.setString(4, project.getExpectedOutputPath());
                pstmt.executeUpdate();
            }

            // 2. Delete existing configurations and results for the project (to overwrite cleanly)
            String deleteConfigs = "DELETE FROM configurations WHERE project_name = ?;";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteConfigs)) {
                pstmt.setString(1, project.getName());
                pstmt.executeUpdate();
            }

            String deleteResults = "DELETE FROM results WHERE project_name = ?;";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteResults)) {
                pstmt.setString(1, project.getName());
                pstmt.executeUpdate();
            }

            // 3. Insert configuration
            if (project.getConfiguration() != null) {
                Configuration cfg = project.getConfiguration();
                String insertConfig = "INSERT INTO configurations (project_name, name, language, source_file_name, " +
                        "compile_command, run_command, run_args, compiled) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
                try (PreparedStatement pstmt = conn.prepareStatement(insertConfig)) {
                    pstmt.setString(1, project.getName());
                    pstmt.setString(2, cfg.getName());
                    pstmt.setString(3, cfg.getLanguage());
                    pstmt.setString(4, cfg.getSourceFileName());
                    pstmt.setString(5, cfg.getCompileCommand());
                    pstmt.setString(6, cfg.getRunCommand());
                    pstmt.setString(7, RunArgsCodec.encode(cfg.getRunArgs()));
                    pstmt.setInt(8, cfg.isCompiled() ? 1 : 0);
                    pstmt.executeUpdate();
                }
            }

            // 4. Insert results
            if (project.getResults() != null && !project.getResults().isEmpty()) {
                String insertResult = "INSERT INTO results (project_name, student_id, status, details, grade) " +
                        "VALUES (?, ?, ?, ?, ?);";
                try (PreparedStatement pstmt = conn.prepareStatement(insertResult)) {
                    for (StudentResult result : project.getResults()) {
                        pstmt.setString(1, project.getName());
                        pstmt.setString(2, result.getStudentId());
                        pstmt.setString(3, result.getStatus().name());
                        pstmt.setString(4, result.getDetails());
                        pstmt.setInt(5, result.getGrade());
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            throw new PersistenceException("Could not save project.", e);
        }
    }

    public Project load(String projectName) {
        Project project = null;

        try (Connection conn = DatabaseHelper.getConnection()) {
            String selectProject = "SELECT * FROM projects WHERE name = ?;";
            try (PreparedStatement pstmt = conn.prepareStatement(selectProject)) {
                pstmt.setString(1, projectName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        project = new Project();
                        project.setName(rs.getString("name"));
                        project.setSubmissionFolder(rs.getString("submission_folder"));
                        project.setReportPath(rs.getString("report_path"));
                        project.setExpectedOutputPath(rs.getString("expected_output_path"));
                    }
                }
            }

            if (project != null) {
                // Load configuration
                String selectConfig = "SELECT * FROM configurations WHERE project_name = ?;";
                try (PreparedStatement pstmt = conn.prepareStatement(selectConfig)) {
                    pstmt.setString(1, projectName);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            Configuration cfg = new Configuration();
                            cfg.setName(rs.getString("name"));
                            cfg.setLanguage(rs.getString("language"));
                            cfg.setSourceFileName(rs.getString("source_file_name"));
                            cfg.setCompileCommand(rs.getString("compile_command"));
                            cfg.setRunCommand(rs.getString("run_command"));
                            cfg.setRunArgs(RunArgsCodec.decode(rs.getString("run_args")));
                            cfg.setCompiled(rs.getInt("compiled") == 1);
                            project.setConfiguration(cfg);
                        }
                    }
                }

                // Load results
                String selectResults = "SELECT * FROM results WHERE project_name = ?;";
                List<StudentResult> results = new ArrayList<>();
                try (PreparedStatement pstmt = conn.prepareStatement(selectResults)) {
                    pstmt.setString(1, projectName);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            TestStatus status = TestStatus.valueOf(rs.getString("status"));
                            int grade = rs.getInt("grade");
                            // Rows saved before the grade column existed are NULL;
                            // fall back to the status-derived score for those.
                            if (rs.wasNull()) {
                                grade = status.score();
                            }
                            StudentResult result = new StudentResult(
                                    rs.getString("student_id"),
                                    status,
                                    rs.getString("details"),
                                    grade
                            );
                            results.add(result);
                        }
                    }
                }
                project.setResults(results);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not load project.", e);
        }

        return project;
    }

    public void delete(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be empty");
        }

        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM configurations WHERE project_name = ?;")) {
                pstmt.setString(1, projectName);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM results WHERE project_name = ?;")) {
                pstmt.setString(1, projectName);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM projects WHERE name = ?;")) {
                pstmt.setString(1, projectName);
                pstmt.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            throw new PersistenceException("Could not delete project.", e);
        }
    }

    public List<String> getAllProjectNames() {
        List<String> names = new ArrayList<>();
        String query = "SELECT name FROM projects;";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not list projects.", e);
        }
        return names;
    }
}

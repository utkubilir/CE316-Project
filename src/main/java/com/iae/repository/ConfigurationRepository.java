package com.iae.repository;

import com.iae.model.Configuration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistence for standalone, reusable Configurations
 * (Requirement #4: create, edit, remove a configuration).
 *
 * Stored in the saved_configurations table, keyed by name.
 */
public class ConfigurationRepository {

    public ConfigurationRepository() {
        DatabaseHelper.initializeDatabase();
    }

    public void save(Configuration cfg) {
        if (cfg == null || cfg.getName() == null || cfg.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration name cannot be empty");
        }

        String sql = "INSERT INTO saved_configurations " +
                "(name, language, source_file_name, compile_command, run_command, run_args, compiled) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(name) DO UPDATE SET " +
                "language=excluded.language, " +
                "source_file_name=excluded.source_file_name, " +
                "compile_command=excluded.compile_command, " +
                "run_command=excluded.run_command, " +
                "run_args=excluded.run_args, " +
                "compiled=excluded.compiled;";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cfg.getName());
            pstmt.setString(2, cfg.getLanguage());
            pstmt.setString(3, cfg.getSourceFileName());
            pstmt.setString(4, cfg.getCompileCommand());
            pstmt.setString(5, cfg.getRunCommand());
            pstmt.setString(6, RunArgsCodec.encode(cfg.getRunArgs()));
            pstmt.setInt(7, cfg.isCompiled() ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Could not save configuration.", e);
        }
    }

    public void delete(String name) {
        String sql = "DELETE FROM saved_configurations WHERE name = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Could not delete configuration.", e);
        }
    }

    public Configuration findByName(String name) {
        String sql = "SELECT * FROM saved_configurations WHERE name = ?;";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return readConfiguration(rs);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not load configuration.", e);
        }
        return null;
    }

    public List<Configuration> findAll() {
        List<Configuration> out = new ArrayList<>();
        String sql = "SELECT * FROM saved_configurations ORDER BY name;";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                out.add(readConfiguration(rs));
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not list configurations.", e);
        }
        return out;
    }

    public List<String> findAllNames() {
        List<String> names = new ArrayList<>();
        String sql = "SELECT name FROM saved_configurations ORDER BY name;";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not list configuration names.", e);
        }
        return names;
    }

    private Configuration readConfiguration(ResultSet rs) throws SQLException {
        Configuration cfg = new Configuration();
        cfg.setName(rs.getString("name"));
        cfg.setLanguage(rs.getString("language"));
        cfg.setSourceFileName(rs.getString("source_file_name"));
        cfg.setCompileCommand(rs.getString("compile_command"));
        cfg.setRunCommand(rs.getString("run_command"));
        cfg.setRunArgs(RunArgsCodec.decode(rs.getString("run_args")));
        cfg.setCompiled(rs.getInt("compiled") == 1);
        return cfg;
    }
}

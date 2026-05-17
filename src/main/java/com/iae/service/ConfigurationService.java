package com.iae.service;

import com.iae.model.Configuration;
import com.iae.repository.ConfigurationRepository;

import java.util.List;

/**
 * Service for working with Configurations.
 *
 * Supports Requirement #4: create, edit and remove configurations.
 * Persistent storage is delegated to {@link ConfigurationRepository};
 * defaults for the supported languages are produced by {@link #createConfiguration}.
 */
public class ConfigurationService {

    private final ConfigurationRepository repository = new ConfigurationRepository();

    /**
     * Build a Configuration pre-filled with reasonable defaults for the given language.
     * Does NOT persist it - callers persist via {@link #saveConfiguration(Configuration)}.
     */
    public Configuration createConfiguration(String name, String language) {

        Configuration config = new Configuration();

        config.setName(name);
        config.setLanguage(language);

        applyLanguageDefaults(config, language);

        return config;
    }

    /** Re-apply default compile/run commands for a language onto an existing configuration. */
    public void applyLanguageDefaults(Configuration config, String language) {
        if (config == null || language == null) {
            return;
        }

        switch (language.toLowerCase()) {

            case "c" -> {
                config.setSourceFileName("main.c");
                config.setCompileCommand("gcc main.c -o main.exe");
                config.setRunCommand("main.exe");
                config.setCompiled(true);
            }

            case "c++" -> {
                config.setSourceFileName("main.cpp");
                config.setCompileCommand("g++ main.cpp -o main.exe");
                config.setRunCommand("main.exe");
                config.setCompiled(true);
            }

            case "java" -> {
                config.setSourceFileName("Main.java");
                config.setCompileCommand("javac Main.java");
                config.setRunCommand("java Main");
                config.setCompiled(true);
            }

            case "python" -> {
                config.setSourceFileName("main.py");
                config.setCompileCommand("");
                config.setRunCommand("python main.py");
                config.setCompiled(false);
            }
        }
    }

    public void saveConfiguration(Configuration config) {
        repository.save(config);
    }

    public void removeConfiguration(String name) {
        repository.delete(name);
    }

    public Configuration getConfiguration(String name) {
        return repository.findByName(name);
    }

    public List<Configuration> listConfigurations() {
        return repository.findAll();
    }

    public List<String> listConfigurationNames() {
        return repository.findAllNames();
    }

    /**
     * On first run there are no saved configurations and the Manage
     * Configurations dialog would be empty. Seed it with sensible defaults
     * for the four supported languages so the lecturer can start with
     * working presets.
     */
    public void seedDefaultsIfEmpty() {
        if (!repository.findAllNames().isEmpty()) {
            return;
        }
        repository.save(createConfiguration("C Configuration",      "C"));
        repository.save(createConfiguration("C++ Configuration",    "C++"));
        repository.save(createConfiguration("Java Configuration",   "Java"));
        repository.save(createConfiguration("Python Configuration", "Python"));
    }
}

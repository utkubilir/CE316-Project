package com.iae.service;

import com.iae.model.Configuration;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationService {

    private final List<Configuration> configurations = new ArrayList<>();

    public Configuration createConfiguration(String name, String language) {

        Configuration config = new Configuration();

        config.setName(name);
        config.setLanguage(language);

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

        configurations.add(config);

        return config;
    }

    public List<Configuration> listConfigurations() {
        return configurations;
    }

    public void removeConfiguration(Configuration configuration) {
        configurations.remove(configuration);
    }
}

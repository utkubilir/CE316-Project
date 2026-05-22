package com.iae.model;

import java.util.ArrayList;
import java.util.List;

public class Configuration {
    private String name;
    private String language;
    private String sourceFileName;
    private String compileCommand;
    private String runCommand;
    private List<String> runArgs = new ArrayList<>();
    private boolean compiled = true;

    public Configuration() {}

    public Configuration(String name, String language) {
        this.name = name;
        this.language = language;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }

    public String getCompileCommand() { return compileCommand; }
    public void setCompileCommand(String compileCommand) { this.compileCommand = compileCommand; }

    public String getRunCommand() { return runCommand; }
    public void setRunCommand(String runCommand) { this.runCommand = runCommand; }

    public List<String> getRunArgs() { return runArgs; }
    public void setRunArgs(List<String> runArgs) { this.runArgs = runArgs != null ? runArgs : new ArrayList<>(); }

    public boolean isCompiled() { return compiled; }
    public void setCompiled(boolean compiled) { this.compiled = compiled; }

    @Override
    public String toString() {
        return name != null ? name : (language != null ? language : "Configuration");
    }
}

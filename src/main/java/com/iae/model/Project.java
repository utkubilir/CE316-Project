package com.iae.model;

import java.util.ArrayList;
import java.util.List;

public class Project {
    private String name;
    private String projectFilePath;
    private String submissionFolder;
    private Configuration configuration = new Configuration();
    private List<StudentResult> results = new ArrayList<>();
    private String reportPath;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProjectFilePath() { return projectFilePath; }
    public void setProjectFilePath(String projectFilePath) { this.projectFilePath = projectFilePath; }

    public String getSubmissionFolder() { return submissionFolder; }
    public void setSubmissionFolder(String submissionFolder) { this.submissionFolder = submissionFolder; }

    public Configuration getConfiguration() { return configuration; }
    public void setConfiguration(Configuration configuration) { this.configuration = configuration; }

    public List<StudentResult> getResults() { return results; }
    public void setResults(List<StudentResult> results) { this.results = results != null ? results : new ArrayList<>(); }

    public String getReportPath() { return reportPath; }
    public void setReportPath(String reportPath) { this.reportPath = reportPath; }
}

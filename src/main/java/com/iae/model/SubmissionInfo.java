package com.iae.model;

/**
 * Row model for the "Student Submissions" table.
 * Populated when the user selects the submissions folder so the lecturer
 * can preview which ZIPs were discovered before running tests.
 */
public class SubmissionInfo {

    private final String fileName;
    private final String studentId;
    private final String size;

    public SubmissionInfo(String fileName, String studentId, String size) {
        this.fileName = fileName;
        this.studentId = studentId;
        this.size = size;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getSize() {
        return size;
    }
}

package com.iae.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.ObjectProperty;

public class StudentResult {
    private final StringProperty studentId = new SimpleStringProperty();
    private final ObjectProperty<TestStatus> status = new SimpleObjectProperty<>(TestStatus.PENDING);
    private final StringProperty details = new SimpleStringProperty("");
    private final IntegerProperty grade = new SimpleIntegerProperty(0);

    public StudentResult() {
    }

    public StudentResult(String studentId, TestStatus status, String details) {
        // Derive the grade from the outcome (tiered scoring, see TestStatus#score).
        this(studentId, status, details, status == null ? 0 : status.score());
    }

    public StudentResult(String studentId, TestStatus status, String details, int grade) {
        this.studentId.set(studentId);
        this.status.set(status);
        this.details.set(details == null ? "" : details);
        this.grade.set(grade);
    }

    public String getStudentId() {
        return studentId.get();
    }

    public void setStudentId(String v) {
        studentId.set(v);
    }

    public StringProperty studentIdProperty() {
        return studentId;
    }

    public TestStatus getStatus() {
        return status.get();
    }

    public void setStatus(TestStatus v) {
        status.set(v);
    }

    public ObjectProperty<TestStatus> statusProperty() {
        return status;
    }

    public String getDetails() {
        return details.get();
    }

    public void setDetails(String v) {
        details.set(v == null ? "" : v);
    }

    public StringProperty detailsProperty() {
        return details;
    }

    public int getGrade() {
        return grade.get();
    }

    public void setGrade(int v) {
        grade.set(v);
    }

    public IntegerProperty gradeProperty() {
        return grade;
    }
}

package com.iae.model;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.ObjectProperty;

public class StudentResult {
    private final StringProperty studentId = new SimpleStringProperty();
    private final ObjectProperty<TestStatus> status = new SimpleObjectProperty<>(TestStatus.PENDING);
    private final StringProperty details = new SimpleStringProperty("");

    public StudentResult() {}

    public StudentResult(String studentId, TestStatus status, String details) {
        this.studentId.set(studentId);
        this.status.set(status);
        this.details.set(details == null ? "" : details);
    }

    public String getStudentId() { return studentId.get(); }
    public void setStudentId(String v) { studentId.set(v); }
    public StringProperty studentIdProperty() { return studentId; }

    public TestStatus getStatus() { return status.get(); }
    public void setStatus(TestStatus v) { status.set(v); }
    public ObjectProperty<TestStatus> statusProperty() { return status; }

    public String getDetails() { return details.get(); }
    public void setDetails(String v) { details.set(v == null ? "" : v); }
    public StringProperty detailsProperty() { return details; }
}

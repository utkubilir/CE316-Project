package com.iae.model;

public enum TestStatus {
    PASSED("Passed"),
    FAILED("Failed"),
    COMPILATION_ERROR("Failed"),
    RUNTIME_ERROR("Failed"),
    OUTPUT_MISMATCH("Failed"),
    EXTRACTION_ERROR("Failed"),
    MISSING_SOURCE("Failed"),
    PENDING("Pending");

    private final String display;

    TestStatus(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }

    public boolean isPassed() {
        return this == PASSED;
    }
}

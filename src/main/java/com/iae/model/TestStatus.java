package com.iae.model;

public enum TestStatus {
    PASSED("Passed"),
    FAILED("Failed"),
    COMPILATION_ERROR("Compile Error"),
    RUNTIME_ERROR("Runtime Error"),
    TIMEOUT("Timeout"),
    OUTPUT_MISMATCH("Output Mismatch"),
    EXTRACTION_ERROR("Extraction Error"),
    MISSING_SOURCE("Missing Source"),
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

package com.iae.model;

public enum TestStatus {
    PASSED("Passed", 100),
    FAILED("Failed", 0),
    COMPILATION_ERROR("Compile Error", 10),
    RUNTIME_ERROR("Runtime Error", 25),
    TIMEOUT("Timeout", 20),
    OUTPUT_MISMATCH("Output Mismatch", 50),
    EXTRACTION_ERROR("Extraction Error", 0),
    MISSING_SOURCE("Missing Source", 0),
    PENDING("Pending", 0);

    private final String display;
    private final int score;

    TestStatus(String display, int score) {
        this.display = display;
        this.score = score;
    }

    public String display() {
        return display;
    }

    public boolean isPassed() {
        return this == PASSED;
    }

    /**
     * Tiered 0-100 grade awarded for a submission that finished with this
     * status. The engine runs a single test per student, so the grade is a
     * function of the outcome category: a full pass scores 100, a near-miss
     * (output mismatch) earns partial credit, error categories earn less, and
     * a missing/unextractable submission scores nothing.
     */
    public int score() {
        return score;
    }
}

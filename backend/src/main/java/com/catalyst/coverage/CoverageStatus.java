package com.catalyst.coverage;

public enum CoverageStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETED;

    /** Completed topics count fully, in-progress ones count half. */
    public double weight() {
        return switch (this) {
            case COMPLETED -> 1.0;
            case IN_PROGRESS -> 0.5;
            case NOT_STARTED -> 0.0;
        };
    }
}

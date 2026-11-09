package com.catalyst.revision.dto;

/** What a review did. Only ADVANCED moves the schedule; the others are recorded as practice. */
public record ReviewResult(RevisionView item, Outcome outcome, String message) {
    public enum Outcome { ADVANCED, PRACTICE_NOT_DUE, PRACTICE_ALREADY_REVIEWED_TODAY, DUPLICATE_REQUEST }
}

package com.catalyst.revision.dto;

import com.catalyst.common.domain.Section;

import java.time.LocalDate;

/**
 * A topic's revision schedule as of today (exam time zone).
 * canAdvanceToday: a review now would move the schedule (it is due or overdue and has not already moved today).
 */
public record RevisionView(String topicId, String topicName, Section section, String chapter, int stage,
                           int intervalDays, LocalDate enrolledOn, LocalDate lastRevisedOn, LocalDate nextDueOn, long daysUntilDue,
                           int reviewCount, Bucket bucket, boolean paused, boolean canAdvanceToday) {
    public enum Bucket { OVERDUE, DUE_TODAY, UPCOMING, PAUSED }
}

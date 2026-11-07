package com.catalyst.revision.dto;

import com.catalyst.common.domain.Section;

import java.time.LocalDate;

public record RevisionView(String topicId, String topicName, Section section, String chapter, int stage,
                           int intervalDays, LocalDate lastRevisedOn, LocalDate nextDueOn, long daysUntilDue,
                           int reviewCount, Bucket bucket) {
    public enum Bucket { OVERDUE, DUE_TODAY, UPCOMING }
}

package com.catalyst.revision.dto;

import com.catalyst.revision.RevisionEvent;

import java.time.Instant;
import java.time.LocalDate;

public record RevisionEventView(String kind, String rating, LocalDate on, Integer priorStage, Integer newStage,
                                LocalDate priorDue, LocalDate newDue, Instant at) {
    public static RevisionEventView of(RevisionEvent e) {
        return new RevisionEventView(e.getKind().name(), e.getRating() == null ? null : e.getRating().name(), e.getOccurredOn(),
                e.getPriorStage(), e.getNewStage(), e.getPriorDue(), e.getNewDue(), e.getCreatedAt());
    }
}

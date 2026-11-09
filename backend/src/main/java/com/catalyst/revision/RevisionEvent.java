package com.catalyst.revision;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** An audit record of one change to (or practice on) a revision item. */
@Entity
@Table(name = "revision_events")
public class RevisionEvent {
    public enum Kind { ENROLL, REVIEW, PRACTICE, PAUSE, RESUME, RESET }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "topic_id", nullable = false)
    private String topicId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;
    @Enumerated(EnumType.STRING)
    private Rating rating;
    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;
    @Column(name = "prior_stage")
    private Integer priorStage;
    @Column(name = "new_stage")
    private Integer newStage;
    @Column(name = "prior_due")
    private LocalDate priorDue;
    @Column(name = "new_due")
    private LocalDate newDue;
    @Column(name = "request_id")
    private String requestId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RevisionEvent() {}

    RevisionEvent(UUID userId, String topicId, Kind kind, Rating rating, LocalDate occurredOn, Integer priorStage, Integer newStage,
                  LocalDate priorDue, LocalDate newDue, String requestId, Instant createdAt) {
        this.userId = userId;
        this.topicId = topicId;
        this.kind = kind;
        this.rating = rating;
        this.occurredOn = occurredOn;
        this.priorStage = priorStage;
        this.newStage = newStage;
        this.priorDue = priorDue;
        this.newDue = newDue;
        this.requestId = requestId;
        this.createdAt = createdAt;
    }

    public Kind getKind() { return kind; }
    public Rating getRating() { return rating; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public Integer getPriorStage() { return priorStage; }
    public Integer getNewStage() { return newStage; }
    public LocalDate getPriorDue() { return priorDue; }
    public LocalDate getNewDue() { return newDue; }
    public Instant getCreatedAt() { return createdAt; }
}

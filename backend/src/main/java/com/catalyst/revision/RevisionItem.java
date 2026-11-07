package com.catalyst.revision;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "revision_items")
public class RevisionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "topic_id", nullable = false)
    private String topicId;
    @Column(nullable = false)
    private int stage;
    @Column(name = "last_revised_on", nullable = false)
    private LocalDate lastRevisedOn;
    @Column(name = "next_due_on", nullable = false)
    private LocalDate nextDueOn;
    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    protected RevisionItem() {}

    public RevisionItem(UUID userId, String topicId, int stage, LocalDate studiedOn, LocalDate nextDueOn) {
        this.userId = userId;
        this.topicId = topicId;
        this.stage = stage;
        this.lastRevisedOn = studiedOn;
        this.nextDueOn = nextDueOn;
        this.reviewCount = 1;
    }

    public void recordReview(int newStage, LocalDate on, LocalDate nextDue) {
        this.stage = newStage;
        this.lastRevisedOn = on;
        this.nextDueOn = nextDue;
        this.reviewCount++;
    }

    public UUID getUserId() { return userId; }
    public String getTopicId() { return topicId; }
    public int getStage() { return stage; }
    public LocalDate getLastRevisedOn() { return lastRevisedOn; }
    public LocalDate getNextDueOn() { return nextDueOn; }
    public int getReviewCount() { return reviewCount; }
}

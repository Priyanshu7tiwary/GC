package com.catalyst.revision;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/** One topic in a learner's revision schedule. All dates are calendar dates in the exam time zone. */
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
    /** Last day the learner revised the topic (a scheduled review or extra practice). */
    @Column(name = "last_revised_on", nullable = false)
    private LocalDate lastRevisedOn;
    @Column(name = "next_due_on", nullable = false)
    private LocalDate nextDueOn;
    /** Number of reviews that moved the schedule. */
    @Column(name = "review_count", nullable = false)
    private int reviewCount;
    @Column(name = "enrolled_on", nullable = false)
    private LocalDate enrolledOn;
    /** Last day a review moved the schedule; a second review that day only counts as practice. */
    @Column(name = "last_advanced_on")
    private LocalDate lastAdvancedOn;
    /** Reopened topics keep their schedule and history but are not due while paused. */
    @Column(nullable = false)
    private boolean paused;

    protected RevisionItem() {}

    public RevisionItem(UUID userId, String topicId, int stage, LocalDate enrolledOn, LocalDate nextDueOn) {
        this.userId = userId;
        this.topicId = topicId;
        this.stage = stage;
        this.enrolledOn = enrolledOn;
        this.lastRevisedOn = enrolledOn;
        this.nextDueOn = nextDueOn;
        this.reviewCount = 0;
    }

    /** True when a review today may move the schedule: not paused, due or overdue, and not already moved today. */
    public boolean canAdvance(LocalDate today) {
        return !paused && !nextDueOn.isAfter(today) && !today.equals(lastAdvancedOn);
    }

    void advance(int newStage, LocalDate today, LocalDate nextDue) {
        this.stage = newStage;
        this.lastRevisedOn = today;
        this.lastAdvancedOn = today;
        this.nextDueOn = nextDue;
        this.reviewCount++;
    }

    void practised(LocalDate today) {
        this.lastRevisedOn = today;
    }

    void pause() {
        this.paused = true;
    }

    /** Resumes with the same stage; a topic that fell due while paused is due again tomorrow. */
    void resume(LocalDate today) {
        this.paused = false;
        if (!nextDueOn.isAfter(today)) nextDueOn = today.plusDays(1);
    }

    void reset(int stage, LocalDate today, LocalDate nextDue) {
        this.stage = stage;
        this.paused = false;
        this.nextDueOn = nextDue;
        this.lastAdvancedOn = null;
    }

    public UUID getUserId() { return userId; }
    public String getTopicId() { return topicId; }
    public int getStage() { return stage; }
    public LocalDate getLastRevisedOn() { return lastRevisedOn; }
    public LocalDate getNextDueOn() { return nextDueOn; }
    public int getReviewCount() { return reviewCount; }
    public LocalDate getEnrolledOn() { return enrolledOn; }
    public LocalDate getLastAdvancedOn() { return lastAdvancedOn; }
    public boolean isPaused() { return paused; }
}

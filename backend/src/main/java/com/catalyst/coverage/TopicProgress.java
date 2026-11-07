package com.catalyst.coverage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "topic_progress")
public class TopicProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "topic_id", nullable = false)
    private String topicId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoverageStatus status;
    @Column(nullable = false)
    private int practiced;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TopicProgress() {}

    public TopicProgress(UUID userId, String topicId, Instant now) {
        this.userId = userId;
        this.topicId = topicId;
        this.status = CoverageStatus.NOT_STARTED;
        this.updatedAt = now;
    }

    public void changeStatus(CoverageStatus status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }

    public void setPracticed(int practiced, Instant now) {
        this.practiced = Math.max(0, practiced);
        if (this.practiced > 0 && status == CoverageStatus.NOT_STARTED) status = CoverageStatus.IN_PROGRESS;
        this.updatedAt = now;
    }

    public UUID getUserId() { return userId; }
    public String getTopicId() { return topicId; }
    public CoverageStatus getStatus() { return status; }
    public int getPracticed() { return practiced; }
}

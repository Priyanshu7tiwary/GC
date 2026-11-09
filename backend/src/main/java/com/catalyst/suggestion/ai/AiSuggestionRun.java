package com.catalyst.suggestion.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One AI study plan a learner generated: counts towards the weekly limit and is kept to show again. */
@Entity
@Table(name = "ai_suggestion_runs")
public class AiSuggestionRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private String response;
    /** False once the limit has been reset: the plan is kept but no longer counts. */
    @Column(nullable = false)
    private boolean counted = true;

    protected AiSuggestionRun() {}

    public AiSuggestionRun(UUID userId, Instant createdAt, String response) {
        this.userId = userId;
        this.createdAt = createdAt;
        this.response = response;
    }

    public Instant getCreatedAt() { return createdAt; }
    public String getResponse() { return response; }
    public boolean isCounted() { return counted; }
}

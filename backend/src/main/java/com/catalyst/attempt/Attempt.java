package com.catalyst.attempt;

import com.catalyst.attempt.scoring.SectionResult;
import com.catalyst.common.domain.Section;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "attempts")
public class Attempt {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "mock_id", nullable = false)
    private String mockId;
    @Column(name = "mock_title", nullable = false)
    private String mockTitle;
    @Column(name = "full_length", nullable = false)
    private boolean fullLength;
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;
    @Column(nullable = false)
    private int score;
    @Column(name = "max_score", nullable = false)
    private int maxScore;
    private Double percentile;
    @Column(name = "time_spent_seconds", nullable = false)
    private int timeSpentSeconds;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "section_results", nullable = false)
    private Map<Section, SectionResult> sectionResults;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, String> answers;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_ids", nullable = false)
    private List<String> questionIds;

    protected Attempt() {}

    public Attempt(UUID userId, String mockId, String mockTitle, boolean fullLength, Instant submittedAt, int score,
                   int maxScore, Double percentile, int timeSpentSeconds, Map<Section, SectionResult> sectionResults,
                   Map<String, String> answers, List<String> questionIds) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.mockId = mockId;
        this.mockTitle = mockTitle;
        this.fullLength = fullLength;
        this.submittedAt = submittedAt;
        this.score = score;
        this.maxScore = maxScore;
        this.percentile = percentile;
        this.timeSpentSeconds = timeSpentSeconds;
        this.sectionResults = sectionResults;
        this.answers = answers;
        this.questionIds = questionIds;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getMockId() { return mockId; }
    public String getMockTitle() { return mockTitle; }
    public boolean isFullLength() { return fullLength; }
    public Instant getSubmittedAt() { return submittedAt; }
    public int getScore() { return score; }
    public int getMaxScore() { return maxScore; }
    public Double getPercentile() { return percentile; }
    public int getTimeSpentSeconds() { return timeSpentSeconds; }
    public Map<Section, SectionResult> getSectionResults() { return sectionResults; }
    public Map<String, String> getAnswers() { return answers; }
    public List<String> getQuestionIds() { return questionIds; }
}

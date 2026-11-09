package com.catalyst.mock;

import com.catalyst.common.domain.Section;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "mocks")
public class Mock {
    @Id
    private String id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MockKind kind;
    @Column(nullable = false)
    private String title;
    private String subtitle;
    @Column(name = "exam_year")
    private Integer examYear;
    private Integer slot;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Section> sections;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "section_question_counts")
    private Map<Section, Integer> sectionQuestionCounts;
    @Column(name = "section_minutes", nullable = false)
    private int sectionMinutes;
    @Column(name = "question_count", nullable = false)
    private int questionCount;
    @Column(nullable = false)
    private String difficulty;
    @Column(name = "owner_id")
    private UUID ownerId;
    /** Explicit question list (custom mocks). Null means "every bank question in these sections". */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_ids")
    private List<String> questionIds;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "focus_topics")
    private List<String> focusTopics;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Mock() {}

    /** Creates a learner-owned custom mock. */
    public static Mock custom(String id, UUID ownerId, String title, String subtitle, List<Section> sections,
                              int sectionMinutes, int questionCount, String difficulty, List<String> questionIds,
                              List<String> focusTopics, Instant now) {
        Mock m = new Mock();
        m.id = id;
        m.kind = MockKind.CUSTOM;
        m.ownerId = ownerId;
        m.title = title;
        m.subtitle = subtitle;
        m.sections = List.copyOf(sections);
        m.sectionMinutes = sectionMinutes;
        m.questionCount = questionCount;
        m.difficulty = difficulty;
        m.questionIds = List.copyOf(questionIds);
        m.focusTopics = List.copyOf(focusTopics);
        m.createdAt = now;
        return m;
    }

    public boolean isFullLength() { return sections.size() == Section.values().length; }

    public boolean isVisibleTo(UUID userId) { return ownerId == null || ownerId.equals(userId); }

    public String getId() { return id; }
    public MockKind getKind() { return kind; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public Integer getExamYear() { return examYear; }
    public Integer getSlot() { return slot; }
    public List<Section> getSections() { return sections; }
    public Map<Section, Integer> getSectionQuestionCounts() { return sectionQuestionCounts; }
    public int getSectionMinutes() { return sectionMinutes; }
    public int getQuestionCount() { return questionCount; }
    public String getDifficulty() { return difficulty; }
    public UUID getOwnerId() { return ownerId; }
    public List<String> getQuestionIds() { return questionIds; }
    public List<String> getFocusTopics() { return focusTopics; }
    public Instant getCreatedAt() { return createdAt; }
}

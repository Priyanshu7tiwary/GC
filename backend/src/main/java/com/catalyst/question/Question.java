package com.catalyst.question;

import com.catalyst.common.domain.Section;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.catalyst.syllabus.Topic;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question {
    @Id
    private String id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Section section;
    @Column(nullable = false)
    private String topic;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;
    @Column(nullable = false)
    private String prompt;
    private String passage;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> options;
    /**
     * For MCQ the zero-based option index; for TITA the literal answer. Never sent to a test-taker.
     * Null for a previous-year question whose key could not be verified: it is shown but not scored.
     */
    private String answer;
    @Column(nullable = false)
    private String explanation;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;
    @Column(nullable = false)
    private int position;
    /** The previous-year mock this question belongs to, or null for the practice bank. */
    @Column(name = "paper_id")
    private String paperId;
    @Column(name = "question_number")
    private Integer questionNumber;
    /** The source paper shows a chart, grid or figure that is not reproduced in the text. */
    @Column(name = "has_figure", nullable = false)
    private boolean hasFigure;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionOrigin origin = QuestionOrigin.BANK;
    @Column(name = "created_at")
    private Instant createdAt;
    /** The syllabus topic whose question bank this question belongs to; null while unassigned. */
    @Column(name = "topic_id")
    private String topicId;
    /** Readable placement, e.g. "QA › Arithmetic › Percentages" (or "QA › Unassigned › <topic>"). */
    @Column(name = "topic_label", nullable = false)
    private String topicLabel;

    protected Question() {}

    /** A new bank question (imported by an admin, or drafted by Claude and verified). Callers validate the key. */
    public static Question create(String id, QuestionOrigin origin, Section section, Topic topic, String topicName, QuestionType type,
                                  String prompt, String passage, List<String> options, String answer, String explanation,
                                  Difficulty difficulty, int position, Instant createdAt) {
        Question q = new Question();
        q.id = id;
        q.origin = origin;
        q.section = section;
        q.type = type;
        q.prompt = prompt;
        q.passage = passage;
        q.options = type == QuestionType.MCQ ? List.copyOf(options) : null;
        q.answer = answer;
        q.explanation = explanation;
        q.difficulty = difficulty;
        q.position = position;
        q.createdAt = createdAt;
        if (topic != null) q.assignTopic(topic);
        else q.unassign(topicName);
        return q;
    }

    /** Moves the question into a syllabus topic's bank. The topic must be in the question's section. */
    public void assignTopic(Topic t) {
        if (t.getSection() != section) throw new IllegalArgumentException("Topic " + t.getId() + " is not a " + section + " topic.");
        this.topic = t.getName();
        this.topicId = t.getId();
        this.topicLabel = label(t);
    }

    private void unassign(String name) {
        this.topic = name == null || name.isBlank() ? "Unassigned" : name.trim();
        this.topicId = null;
        this.topicLabel = section + " › Unassigned › " + this.topic;
    }

    public static String label(Topic t) {
        return t.getSection() + " › " + t.getChapter() + " › " + t.getName();
    }

    public String getId() { return id; }
    public Section getSection() { return section; }
    public String getTopic() { return topic; }
    public QuestionType getType() { return type; }
    public String getPrompt() { return prompt; }
    public String getPassage() { return passage; }
    public List<String> getOptions() { return options; }
    public String getAnswer() { return answer; }
    public String getExplanation() { return explanation; }
    public Difficulty getDifficulty() { return difficulty; }
    public int getPosition() { return position; }
    public String getPaperId() { return paperId; }
    public Integer getQuestionNumber() { return questionNumber; }
    public boolean hasFigure() { return hasFigure; }
    public boolean isScored() { return answer != null; }
    public QuestionOrigin getOrigin() { return origin; }
    public String getTopicId() { return topicId; }
    public String getTopicLabel() { return topicLabel; }
    public Instant getCreatedAt() { return createdAt; }

    /** Admin correction of the answer key; callers validate the value for the question type. */
    public void replaceAnswer(String answer) { this.answer = answer; }
}

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
    /** For MCQ the zero-based option index; for TITA the literal answer. Never sent to a test-taker. */
    @Column(nullable = false)
    private String answer;
    @Column(nullable = false)
    private String explanation;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;
    @Column(nullable = false)
    private int position;

    protected Question() {}

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
}

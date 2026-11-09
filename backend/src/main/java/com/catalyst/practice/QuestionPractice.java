package com.catalyst.practice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One learner's practice state for one question. */
@Entity
@Table(name = "question_practice")
public class QuestionPractice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "question_id", nullable = false)
    private String questionId;
    private boolean solved;
    private boolean marked;
    private int attempts;
    @Column(name = "last_answer")
    private String lastAnswer;
    @Column(name = "last_correct")
    private Boolean lastCorrect;
    @Column(name = "solved_at")
    private Instant solvedAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected QuestionPractice() {}

    public QuestionPractice(UUID userId, String questionId, Instant now) {
        this.userId = userId;
        this.questionId = questionId;
        this.updatedAt = now;
    }

    /** Records a checked answer; a correct one also marks the question solved. */
    void checked(String answer, boolean correct, Instant now) {
        attempts++;
        lastAnswer = answer;
        lastCorrect = correct;
        if (correct) setSolved(true, now);
        updatedAt = now;
    }

    void setSolved(boolean value, Instant now) {
        if (value && !solved) solvedAt = now;
        if (!value) solvedAt = null;
        solved = value;
        updatedAt = now;
    }

    void setMarked(boolean value, Instant now) {
        marked = value;
        updatedAt = now;
    }

    public String getQuestionId() { return questionId; }
    public boolean isSolved() { return solved; }
    public boolean isMarked() { return marked; }
    public int getAttempts() { return attempts; }
    public String getLastAnswer() { return lastAnswer; }
    public Boolean getLastCorrect() { return lastCorrect; }
    public Instant getSolvedAt() { return solvedAt; }
}

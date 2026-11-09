package com.catalyst.answerkey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One admin change to a question's answer key, kept as an audit trail. */
@Entity
@Table(name = "answer_key_corrections")
public class AnswerKeyCorrection {
    @Id
    private UUID id;
    @Column(name = "question_id", nullable = false)
    private String questionId;
    @Column(name = "previous_answer")
    private String previousAnswer;
    @Column(name = "new_answer", nullable = false)
    private String newAnswer;
    @Column(name = "corrected_by")
    private UUID correctedBy;
    @Column(name = "corrected_at", nullable = false)
    private Instant correctedAt;

    protected AnswerKeyCorrection() {}

    public AnswerKeyCorrection(String questionId, String previousAnswer, String newAnswer, UUID correctedBy, Instant correctedAt) {
        this.id = UUID.randomUUID();
        this.questionId = questionId;
        this.previousAnswer = previousAnswer;
        this.newAnswer = newAnswer;
        this.correctedBy = correctedBy;
        this.correctedAt = correctedAt;
    }

    public String getQuestionId() { return questionId; }
    public String getPreviousAnswer() { return previousAnswer; }
    public String getNewAnswer() { return newAnswer; }
    public Instant getCorrectedAt() { return correctedAt; }
}

package com.catalyst.attempt.dto;

import com.catalyst.attempt.scoring.Outcome;
import com.catalyst.common.domain.Section;
import com.catalyst.question.QuestionType;

import java.util.List;

/** An attempt with full solutions: correct answer, the learner's response, outcome, and explanation. */
public record AttemptDetail(AttemptSummary summary, List<Solution> solutions) {
    public record Solution(String questionId, Section section, String topic, QuestionType type, String prompt,
                           String passage, List<String> options, String correctAnswer, String response,
                           Outcome outcome, int marks, String explanation) {}
}

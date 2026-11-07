package com.catalyst.attempt.scoring;

import com.catalyst.question.Question;
import com.catalyst.question.QuestionType;
import org.springframework.stereotype.Component;

/** Responses are the zero-based index of the chosen option. */
@Component
class McqAnswerEvaluator implements AnswerEvaluator {
    @Override
    public QuestionType supports() {
        return QuestionType.MCQ;
    }

    @Override
    public boolean isCorrect(Question q, String response) {
        try {
            return Integer.parseInt(response.trim()) == Integer.parseInt(q.getAnswer().trim());
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

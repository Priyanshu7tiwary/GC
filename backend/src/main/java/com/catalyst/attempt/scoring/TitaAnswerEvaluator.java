package com.catalyst.attempt.scoring;

import com.catalyst.question.Question;
import com.catalyst.question.QuestionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Numeric answers compare by value (so "48" equals "48.0"); sequences like "2413" compare as text. */
@Component
class TitaAnswerEvaluator implements AnswerEvaluator {
    @Override
    public QuestionType supports() {
        return QuestionType.TITA;
    }

    @Override
    public boolean isCorrect(Question q, String response) {
        String given = response.trim(), key = q.getAnswer().trim();
        if (given.equalsIgnoreCase(key)) return true;
        try {
            return new BigDecimal(given).compareTo(new BigDecimal(key)) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

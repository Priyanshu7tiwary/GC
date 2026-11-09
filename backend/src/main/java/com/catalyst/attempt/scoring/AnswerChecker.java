package com.catalyst.attempt.scoring;

import com.catalyst.question.Question;
import com.catalyst.question.QuestionType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Routes each question to the evaluator for its type. */
@Component
public class AnswerChecker {
    private final Map<QuestionType, AnswerEvaluator> evaluators = new EnumMap<>(QuestionType.class);

    public AnswerChecker(List<AnswerEvaluator> all) {
        all.forEach(e -> evaluators.put(e.supports(), e));
        for (QuestionType t : QuestionType.values()) {
            if (!evaluators.containsKey(t)) throw new IllegalStateException("No AnswerEvaluator for " + t);
        }
    }

    public Outcome check(Question q, String response) {
        if (!q.isScored()) return Outcome.UNSCORED;
        if (response == null || response.isBlank()) return Outcome.SKIPPED;
        return evaluators.get(q.getType()).isCorrect(q, response) ? Outcome.CORRECT : Outcome.WRONG;
    }
}

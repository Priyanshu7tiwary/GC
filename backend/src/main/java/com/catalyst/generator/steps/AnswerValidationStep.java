package com.catalyst.generator.steps;

import com.catalyst.generator.GenerationContext;
import com.catalyst.generator.GenerationStep;
import com.catalyst.question.Question;
import com.catalyst.question.QuestionType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** Drops any question whose answer key is malformed, and fails if a section ends up empty. */
@Component
@Order(40)
class AnswerValidationStep implements GenerationStep {
    public String id() { return "validate"; }
    public String label() { return "Validating answers"; }
    public String detail() { return "Checking every answer key before it reaches you"; }

    @Override
    public String execute(GenerationContext ctx) {
        int total = 0, valid = 0;
        for (var entry : ctx.questions().entrySet()) {
            List<Question> ok = entry.getValue().stream().filter(AnswerValidationStep::hasValidKey).toList();
            total += entry.getValue().size();
            valid += ok.size();
            if (ok.isEmpty()) throw new IllegalStateException("No usable questions for " + entry.getKey() + ".");
            entry.setValue(ok);
        }
        return valid + "/" + total + " answer keys verified";
    }

    static boolean hasValidKey(Question q) {
        if (q.getAnswer() == null || q.getAnswer().isBlank()) return false;
        if (q.getType() != QuestionType.MCQ) return true;
        try {
            int idx = Integer.parseInt(q.getAnswer().trim());
            return q.getOptions() != null && idx >= 0 && idx < q.getOptions().size();
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

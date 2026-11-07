package com.catalyst.attempt.scoring;

import com.catalyst.question.QuestionType;
import org.springframework.stereotype.Component;

/** CAT: +3 for any correct answer, −1 for a wrong MCQ, no penalty on TITA. */
@Component
class CatMarkingScheme implements MarkingScheme {
    @Override
    public int correct(QuestionType type) {
        return 3;
    }

    @Override
    public int wrong(QuestionType type) {
        return type == QuestionType.MCQ ? -1 : 0;
    }
}

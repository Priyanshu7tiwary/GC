package com.catalyst.attempt.scoring;

import com.catalyst.question.QuestionType;

public interface MarkingScheme {
    int correct(QuestionType type);

    /** Marks for a wrong answer; zero or negative. */
    int wrong(QuestionType type);

    default int marks(QuestionType type, Outcome outcome) {
        return switch (outcome) {
            case CORRECT -> correct(type);
            case WRONG -> wrong(type);
            case SKIPPED, UNSCORED -> 0;
        };
    }
}

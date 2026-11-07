package com.catalyst.attempt.scoring;

import com.catalyst.question.Question;
import com.catalyst.question.QuestionType;

/** Checks a non-blank response for one question type. Add a new type by adding an implementation. */
public interface AnswerEvaluator {
    QuestionType supports();

    boolean isCorrect(Question question, String response);
}

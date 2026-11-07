package com.catalyst.question.dto;

import com.catalyst.common.domain.Section;
import com.catalyst.question.Question;
import com.catalyst.question.QuestionType;

import java.util.List;

/** A question as shown during a test: no answer key, no explanation. */
public record QuestionView(String id, Section section, String topic, QuestionType type, String prompt, String passage,
                           List<String> options) {
    public static QuestionView of(Question q) {
        return new QuestionView(q.getId(), q.getSection(), q.getTopic(), q.getType(), q.getPrompt(), q.getPassage(), q.getOptions());
    }
}

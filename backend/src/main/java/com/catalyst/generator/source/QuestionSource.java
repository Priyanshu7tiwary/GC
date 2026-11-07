package com.catalyst.generator.source;

import com.catalyst.common.domain.Section;
import com.catalyst.generator.TargetDifficulty;
import com.catalyst.question.Question;

import java.util.List;

/** Where generated mocks get their questions. Today: the question bank. Later: an LLM-backed source. */
public interface QuestionSource {
    List<Question> fetch(Section section, int count, TargetDifficulty difficulty, List<String> preferredTopics);
}

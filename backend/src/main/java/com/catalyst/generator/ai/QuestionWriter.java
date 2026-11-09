package com.catalyst.generator.ai;

import com.catalyst.common.domain.Section;

import java.util.List;
import java.util.Optional;

/** Drafts new CAT-style questions and solves them independently. Absent when no model is configured. */
public interface QuestionWriter {
    /** New questions for a section; empty when the model is unavailable or declines. */
    List<DraftQuestion> draft(Section section, int count, List<String> topics, List<String> styleExamples);

    /** An independent answer for each draft (same order); empty entries where the solver gave none. */
    List<Optional<String>> solve(List<DraftQuestion> drafts);
}

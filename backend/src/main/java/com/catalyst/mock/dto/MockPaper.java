package com.catalyst.mock.dto;

import com.catalyst.common.domain.Section;
import com.catalyst.question.dto.QuestionView;

import java.util.List;

/** What the test player needs: sections in order, each with its time limit and questions (answers withheld). */
public record MockPaper(String mockId, String title, int sectionMinutes, MarkingInfo marking, List<PaperSection> sections) {
    public record PaperSection(Section section, List<QuestionView> questions) {}

    public record MarkingInfo(int correct, int wrongMcq, int wrongTita) {}
}

package com.catalyst.generator.steps;

import com.catalyst.common.domain.Section;
import com.catalyst.generator.plan.CoveragePickerTest;
import com.catalyst.question.Question;
import com.catalyst.question.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReplaceableTest {
    @Test
    void aVerifiedAiQuestionReplacesOneFromTheMostRepresentedTopic() {
        List<Question> chosen = List.of(
                CoveragePickerTest.q("a1", Section.QA, "a", QuestionType.MCQ, null),
                CoveragePickerTest.q("b1", Section.QA, "b", QuestionType.MCQ, null),
                CoveragePickerTest.q("b2", Section.QA, "b", QuestionType.MCQ, null),
                CoveragePickerTest.q("c1", Section.QA, "c", QuestionType.MCQ, null));
        assertThat(chosen.get(AnswerValidationStep.replaceable(chosen)).getTopicId()).isEqualTo("b");
    }
}

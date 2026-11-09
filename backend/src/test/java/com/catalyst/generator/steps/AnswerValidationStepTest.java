package com.catalyst.generator.steps;

import com.catalyst.generator.ai.DraftQuestion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerValidationStepTest {
    private static DraftQuestion mcq(List<String> options, String answer) {
        return new DraftQuestion("MCQ", "Percentages", "What is 10% of 50?", options, answer, "", "EASY");
    }

    @Test
    void rejectsMalformedDrafts() {
        assertThat(AnswerValidationStep.wellFormed(mcq(List.of("5", "10", "15", "20"), "0"))).isTrue();
        assertThat(AnswerValidationStep.wellFormed(mcq(List.of("5", "10", "15"), "0"))).as("three options").isFalse();
        assertThat(AnswerValidationStep.wellFormed(mcq(List.of("5", "5", "15", "20"), "0"))).as("duplicate options").isFalse();
        assertThat(AnswerValidationStep.wellFormed(mcq(List.of("5", "10", "15", "20"), "4"))).as("index out of range").isFalse();
        assertThat(AnswerValidationStep.wellFormed(new DraftQuestion("TITA", "Averages", "Find x.", List.of(), "4.5", "", "HARD"))).isTrue();
        assertThat(AnswerValidationStep.wellFormed(new DraftQuestion("TITA", "Averages", "Find x.", List.of(), "about 4", "", "HARD"))).isFalse();
    }

    @Test
    void comparesSolvedAnswersByValue() {
        DraftQuestion tita = new DraftQuestion("TITA", "Averages", "Find x.", List.of(), "48", "", "HARD");
        assertThat(AnswerValidationStep.sameAnswer(tita, "48.0")).isTrue();
        assertThat(AnswerValidationStep.sameAnswer(tita, "47")).isFalse();
        assertThat(AnswerValidationStep.sameAnswer(mcq(List.of("a", "b", "c", "d"), "2"), "2")).isTrue();
    }
}

package com.catalyst.question.admin;

import com.catalyst.common.domain.Section;
import com.catalyst.question.Difficulty;
import com.catalyst.question.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * One question to add to the bank.
 * answer: MCQ — the zero-based index of the correct option ("0"–"3"); TITA — the exact answer (a number, or a sequence such as 2413).
 * passage: shared text for RC / DILR sets; questions with identical passage text are kept together in generated mocks.
 * Maths may be written as TeX between \( and \).
 */
public record ImportQuestion(
        @NotNull Section section,
        @NotNull QuestionType type,
        @NotBlank @Size(max = 120) String topic,
        @NotBlank @Size(max = 4000) String prompt,
        @Size(max = 20000) String passage,
        @Size(max = 6) List<@NotBlank @Size(max = 500) String> options,
        @NotBlank @Size(max = 64) String answer,
        @Size(max = 4000) String explanation,
        Difficulty difficulty) {}

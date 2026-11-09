package com.catalyst.answerkey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** MCQ: the zero-based index of the correct option. TITA: the exact answer a candidate should type. */
public record CorrectAnswerRequest(@NotBlank @Size(max = 64) String answer) {}

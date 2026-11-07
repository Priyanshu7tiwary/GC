package com.catalyst.attempt.dto;

import com.catalyst.common.domain.Section;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * answers: questionId → response (MCQ: option index as text, e.g. "2"; TITA: the typed answer).
 * sectionSeconds: time actually spent per section.
 */
public record SubmitAttemptRequest(
        @NotNull @Size(max = 500) Map<@Size(max = 40) String, @Size(max = 64) String> answers,
        Map<Section, @Min(0) @Max(14_400) Integer> sectionSeconds) {}

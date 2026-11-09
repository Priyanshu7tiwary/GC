package com.catalyst.attempt.dto;

import com.catalyst.common.domain.Section;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * answers: questionId → response (MCQ: option index as text, e.g. "2"; TITA: the typed answer).
 * sectionSeconds: time actually spent per section.
 * sections: the sections actually taken, when only part of the mock was attempted (default: all of them).
 */
public record SubmitAttemptRequest(
        @NotNull @Size(max = 500) Map<@Size(max = 40) String, @Size(max = 64) String> answers,
        Map<Section, @Min(0) @Max(14_400) Integer> sectionSeconds,
        @Size(max = 3) List<Section> sections) {

    public SubmitAttemptRequest(Map<String, String> answers, Map<Section, Integer> sectionSeconds) {
        this(answers, sectionSeconds, null);
    }
}

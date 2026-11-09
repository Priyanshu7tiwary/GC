package com.catalyst.suggestion.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * What the browser knows about the learner (coverage, mock attempts and revision backlog live there). Only ids and
 * numbers are accepted, never names or emails, and every collection is bounded.
 */
public record AiSuggestionRequest(
        @Min(50) @Max(100) double targetPercentile,
        @Min(2024) @Max(2100) int targetYear,
        @Min(0) @Max(5000) int daysLeft,
        @Size(max = 120) Map<@Size(max = 160) String, @Min(0) @Max(2) Integer> topicStatus,
        @Size(max = 8) List<@Valid @NotNull Attempt> recentAttempts,
        @Size(max = 300) List<@Size(max = 80) String> attemptedMockIds,
        @Size(max = 80) List<@Size(max = 160) String> overdueTopicIds,
        @Min(0) @Max(100000) int minutesLast7Days,
        @Min(0) @Max(100000) int minutesPrevious7Days) {

    public record Attempt(@Size(max = 80) String mockId, @Size(max = 160) String title, @Min(-1000) @Max(1000) int score, @Min(0) @Max(1000) int max,
                          @Size(max = 3) Map<@Size(max = 8) String, @Valid SectionResult> sections) {}

    public record SectionResult(@Min(0) @Max(200) int correct, @Min(0) @Max(200) int wrong, @Min(0) @Max(200) int unattempted,
                                @Min(-500) @Max(500) int score, @Min(0) @Max(10000) int minutes) {}
}

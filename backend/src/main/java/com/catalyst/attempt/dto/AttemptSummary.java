package com.catalyst.attempt.dto;

import com.catalyst.attempt.Attempt;
import com.catalyst.attempt.scoring.SectionResult;
import com.catalyst.common.domain.Section;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AttemptSummary(UUID id, String mockId, String title, boolean fullLength, Instant submittedAt, int score,
                             int maxScore, Double percentile, int timeSpentSeconds, Map<Section, SectionResult> sections) {
    public static AttemptSummary of(Attempt a) {
        return new AttemptSummary(a.getId(), a.getMockId(), a.getMockTitle(), a.isFullLength(), a.getSubmittedAt(),
                a.getScore(), a.getMaxScore(), a.getPercentile(), a.getTimeSpentSeconds(), a.getSectionResults());
    }
}

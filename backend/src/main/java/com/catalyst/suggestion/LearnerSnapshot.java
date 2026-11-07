package com.catalyst.suggestion;

import com.catalyst.activity.dto.ActivityView;
import com.catalyst.attempt.dto.AttemptSummary;
import com.catalyst.attempt.scoring.SectionResult;
import com.catalyst.common.domain.Section;
import com.catalyst.coverage.dto.CoverageView.TopicCoverage;
import com.catalyst.revision.dto.RevisionView;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

/** Everything a suggestion rule may look at, gathered once per request. Read-only. */
public record LearnerSnapshot(LocalDate today, double targetPercentile, int targetYear, long daysLeft,
                              double requiredScoreRatio, List<AttemptSummary> fullMocks, List<TopicCoverage> coverage,
                              List<RevisionView> revisions, ActivityView activity) {

    public static final int RECENT_WINDOW = 4;

    public List<AttemptSummary> recentFullMocks() {
        return fullMocks.subList(Math.max(0, fullMocks.size() - RECENT_WINDOW), fullMocks.size());
    }

    public boolean longRunway() {
        return daysLeft > 180;
    }

    /** Average of some per-section measure over the recent full mocks. */
    public double recentAverage(Section s, ToDoubleFunction<SectionResult> f) {
        return recentFullMocks().stream().map(a -> a.sections().get(s)).filter(Objects::nonNull)
                .mapToDouble(f).average().orElse(0);
    }
}

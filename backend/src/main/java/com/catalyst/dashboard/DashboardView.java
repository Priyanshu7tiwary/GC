package com.catalyst.dashboard;

import com.catalyst.attempt.dto.AttemptSummary;
import com.catalyst.common.domain.Section;
import com.catalyst.coverage.dto.CoverageView;
import com.catalyst.mock.dto.MockSummary;
import com.catalyst.revision.dto.RevisionView;
import com.catalyst.user.dto.ProfileResponse;

import java.util.List;
import java.util.Map;

/** Everything the home screen needs in one round trip. */
public record DashboardView(ProfileResponse profile, Kpis kpis, List<TrendPoint> sectionTrend,
                            List<RevisionView> dueRevisions, MockSummary nextMock,
                            Map<Section, CoverageView.Stats> coverageBySection,
                            List<CoverageView.TopicCoverage> focusTopics, List<AttemptSummary> recentAttempts,
                            int highPrioritySuggestions) {

    public record Kpis(Integer latestScore, Integer latestMax, Double percentileChange, Double bestPercentile,
                       int fullMocksTaken, int coveragePercent, int topicsCompleted, int topicsInProgress,
                       int topicsTotal, int streakDays, int minutesLast7Days, int revisionsDue) {}

    /** One full mock: each section's score as a % of that section's maximum. */
    public record TrendPoint(String label, String title, Map<Section, Integer> percentOfMax) {}
}

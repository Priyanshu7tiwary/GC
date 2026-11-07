package com.catalyst.dashboard;

import com.catalyst.activity.ActivityService;
import com.catalyst.attempt.AttemptService;
import com.catalyst.attempt.dto.AttemptSummary;
import com.catalyst.attempt.scoring.SectionResult;
import com.catalyst.common.domain.Section;
import com.catalyst.coverage.CoverageService;
import com.catalyst.coverage.CoverageStatus;
import com.catalyst.coverage.dto.CoverageView;
import com.catalyst.mock.MockCatalogService;
import com.catalyst.mock.MockKind;
import com.catalyst.revision.RevisionService;
import com.catalyst.revision.dto.RevisionView.Bucket;
import com.catalyst.suggestion.Priority;
import com.catalyst.suggestion.SuggestionService;
import com.catalyst.user.ProfileService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;

@Service
public class DashboardService {
    private final ProfileService profiles;
    private final AttemptService attempts;
    private final CoverageService coverage;
    private final RevisionService revisions;
    private final ActivityService activity;
    private final MockCatalogService mocks;
    private final SuggestionService suggestions;

    public DashboardService(ProfileService profiles, AttemptService attempts, CoverageService coverage, RevisionService revisions,
                            ActivityService activity, MockCatalogService mocks, SuggestionService suggestions) {
        this.profiles = profiles;
        this.attempts = attempts;
        this.coverage = coverage;
        this.revisions = revisions;
        this.activity = activity;
        this.mocks = mocks;
        this.suggestions = suggestions;
    }

    public DashboardView dashboard(UUID userId) {
        List<AttemptSummary> all = attempts.list(userId);
        List<AttemptSummary> fulls = all.stream().filter(AttemptSummary::fullLength).toList();
        AttemptSummary last = fulls.isEmpty() ? null : fulls.getLast();
        AttemptSummary prev = fulls.size() < 2 ? null : fulls.get(fulls.size() - 2);
        CoverageView cov = coverage.view(userId);
        var act = activity.view(userId, 7);
        var due = revisions.list(userId).stream().filter(r -> r.bucket() != Bucket.UPCOMING).toList();

        var kpis = new DashboardView.Kpis(
                last == null ? null : last.score(), last == null ? null : last.maxScore(),
                last != null && prev != null && last.percentile() != null && prev.percentile() != null
                        ? Math.round((last.percentile() - prev.percentile()) * 10) / 10.0 : null,
                fulls.stream().map(AttemptSummary::percentile).filter(java.util.Objects::nonNull).max(Double::compare).orElse(null),
                fulls.size(), cov.overall().percent(), cov.overall().completed(), cov.overall().inProgress(), cov.overall().total(),
                act.streakDays(), act.minutesLast7Days(), due.size());

        List<AttemptSummary> window = fulls.subList(Math.max(0, fulls.size() - 8), fulls.size());
        var trend = IntStream.range(0, window.size()).mapToObj(i -> new DashboardView.TrendPoint("M" + (i + 1), window.get(i).title(),
                perSection(window.get(i), r -> r.maxScore() == 0 ? 0 : (int) Math.max(0, Math.round(100.0 * r.score() / r.maxScore()))))).toList();

        var nextMock = mocks.list(userId, MockKind.PREVIOUS_YEAR, null, null).stream()
                .filter(m -> m.progress().attempts() == 0).findFirst().orElse(null);
        var focus = cov.topics().stream().filter(t -> t.status() != CoverageStatus.COMPLETED)
                .sorted(Comparator.comparingInt(CoverageView.TopicCoverage::practiced)).limit(5).toList();
        int high = (int) suggestions.suggestionsFor(userId).stream().filter(s -> s.priority() == Priority.HIGH).count();

        return new DashboardView(profiles.get(userId), kpis, trend, due.stream().limit(4).toList(), nextMock,
                cov.bySection(), focus, all.reversed().stream().limit(4).toList(), high);
    }

    public AnalyticsView analytics(UUID userId) {
        List<AttemptSummary> fulls = attempts.list(userId).stream().filter(AttemptSummary::fullLength).toList();
        var pct = IntStream.range(0, fulls.size()).mapToObj(i -> new AnalyticsView.PercentilePoint("M" + (i + 1), fulls.get(i).title(),
                fulls.get(i).percentile() == null ? 0 : fulls.get(i).percentile())).toList();
        var acc = IntStream.range(0, fulls.size()).mapToObj(i -> new AnalyticsView.AccuracyPoint("M" + (i + 1), fulls.get(i).title(),
                perSection(fulls.get(i), r -> (int) Math.round(r.accuracy() * 100)))).toList();
        var averages = java.util.Arrays.stream(Section.values()).map(s -> {
            var list = fulls.stream().map(a -> a.sections().get(s)).filter(java.util.Objects::nonNull).toList();
            return new AnalyticsView.SectionAverage(s,
                    avg(list, SectionResult::score), avg(list, SectionResult::attempted),
                    avg(list, r -> r.accuracy() * 100), avg(list, SectionResult::wrong));
        }).toList();
        return new AnalyticsView(pct, acc, averages);
    }

    private static Map<Section, Integer> perSection(AttemptSummary a, Function<SectionResult, Integer> f) {
        Map<Section, Integer> m = new EnumMap<>(Section.class);
        a.sections().forEach((s, r) -> m.put(s, f.apply(r)));
        return m;
    }

    private static double avg(List<SectionResult> list, java.util.function.ToDoubleFunction<SectionResult> f) {
        return Math.round(list.stream().mapToDouble(f).average().orElse(0) * 10) / 10.0;
    }
}

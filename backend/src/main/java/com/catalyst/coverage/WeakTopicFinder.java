package com.catalyst.coverage;

import com.catalyst.common.domain.Section;
import com.catalyst.coverage.dto.CoverageView.TopicCoverage;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Finds the topics a learner is weakest on: not yet completed, least practised first. */
@Component
public class WeakTopicFinder {
    private final CoverageService coverage;

    public WeakTopicFinder(CoverageService coverage) {
        this.coverage = coverage;
    }

    public List<TopicCoverage> find(UUID userId, Collection<Section> sections, int limit) {
        return coverage.topicsFor(userId).stream()
                .filter(t -> sections.contains(t.section()) && t.status() != CoverageStatus.COMPLETED)
                .sorted(Comparator.comparingInt(TopicCoverage::practiced))
                .limit(limit)
                .toList();
    }
}

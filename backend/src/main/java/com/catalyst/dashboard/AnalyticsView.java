package com.catalyst.dashboard;

import com.catalyst.common.domain.Section;

import java.util.List;
import java.util.Map;

public record AnalyticsView(List<PercentilePoint> percentileTrend, List<AccuracyPoint> accuracyTrend,
                            List<SectionAverage> sectionAverages) {
    public record PercentilePoint(String label, String title, double percentile) {}

    public record AccuracyPoint(String label, String title, Map<Section, Integer> accuracyPercent) {}

    public record SectionAverage(Section section, double score, double attempted, double accuracyPercent, double negatives) {}
}

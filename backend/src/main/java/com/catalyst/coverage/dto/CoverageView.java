package com.catalyst.coverage.dto;

import com.catalyst.common.domain.Section;
import com.catalyst.coverage.CoverageStatus;

import java.util.List;
import java.util.Map;

public record CoverageView(Stats overall, Map<Section, Stats> bySection, List<TopicCoverage> topics) {
    public record Stats(int total, int completed, int inProgress, int percent, int questionsPracticed) {}

    public record TopicCoverage(String topicId, Section section, String chapter, String name, boolean highYield,
                                CoverageStatus status, int practiced) {}
}

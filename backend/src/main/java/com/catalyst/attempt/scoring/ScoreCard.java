package com.catalyst.attempt.scoring;

import com.catalyst.common.domain.Section;

import java.util.Map;

public record ScoreCard(int score, int maxScore, Map<Section, SectionResult> sections, Map<String, Outcome> outcomes) {}

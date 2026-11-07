package com.catalyst.mock.dto;

import com.catalyst.common.domain.Section;
import com.catalyst.mock.AttemptStats;
import com.catalyst.mock.Mock;
import com.catalyst.mock.MockKind;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MockSummary(String id, MockKind kind, String title, String subtitle, Integer year, Integer slot,
                          List<Section> sections, Map<Section, Integer> sectionQuestionCounts, int questionCount,
                          int sectionMinutes, int totalMinutes, String difficulty, List<String> focusTopics,
                          Instant createdAt, String paperAssetPath, String paperSourceUrl,
                          String paperSourceStatus, AttemptStats progress) {
    public static MockSummary of(Mock m, AttemptStats stats) {
        return new MockSummary(m.getId(), m.getKind(), m.getTitle(), m.getSubtitle(), m.getExamYear(), m.getSlot(),
                m.getSections(), m.getSectionQuestionCounts(), m.getQuestionCount(), m.getSectionMinutes(),
                m.getSectionMinutes() * m.getSections().size(), m.getDifficulty(), m.getFocusTopics(),
                m.getKind() == MockKind.CUSTOM ? m.getCreatedAt() : null,
                m.getPaperAssetPath(), m.getPaperSourceUrl(), m.getPaperSourceStatus(),
                stats == null ? new AttemptStats(0, 0, null, null) : stats);
    }
}

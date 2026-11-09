package com.catalyst.generator.plan;

import com.catalyst.common.domain.Section;

import java.util.List;

/**
 * What one section of a generated test should contain.
 *
 * passageQuestions: questions to take from passage sets (RC passages in VARC, sets in DILR), whole sets where possible.
 * setTopics: DILR only — the topics the sets should cover, one set each, in priority order.
 * slots: standalone questions per topic (QA, and verbal ability in VARC).
 */
public record SectionPlan(Section section, int total, int passageQuestions, List<Slot> setTopics, List<Slot> slots) {

    /** count questions (or, for a set topic, one set) from a topic. */
    public record Slot(String topicId, String topic, String chapter, int count) {}
}

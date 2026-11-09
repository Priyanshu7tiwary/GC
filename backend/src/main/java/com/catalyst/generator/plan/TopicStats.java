package com.catalyst.generator.plan;

import com.catalyst.common.domain.Section;

/**
 * One syllabus topic as the planner sees it.
 * pyqWeight: how often the topic appears in official CAT papers (the planner's model of the real exam).
 * available: keyed questions in the database for this topic.
 */
public record TopicStats(String id, Section section, String chapter, String name, long pyqWeight, long available) {}

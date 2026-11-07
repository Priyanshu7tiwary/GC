package com.catalyst.events;

import java.util.UUID;

/** A learner marked a syllabus topic as completed. */
public record TopicCompletedEvent(UUID userId, String topicId) {}

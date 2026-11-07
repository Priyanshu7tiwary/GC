package com.catalyst.events;

import java.util.UUID;

/** A previously completed topic was moved back to "not started" or "in progress". */
public record TopicReopenedEvent(UUID userId, String topicId) {}

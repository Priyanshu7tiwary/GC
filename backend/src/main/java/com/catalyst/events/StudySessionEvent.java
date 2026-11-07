package com.catalyst.events;

import java.util.UUID;

/** Some study happened (a revision, a mock). Used to build the activity log and streaks. */
public record StudySessionEvent(UUID userId, int minutes) {}

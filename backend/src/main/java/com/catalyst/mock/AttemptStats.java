package com.catalyst.mock;

import java.util.UUID;

/** A learner's track record on one mock. */
public record AttemptStats(int attempts, int bestScore, UUID lastAttemptId, Double lastPercentile) {}

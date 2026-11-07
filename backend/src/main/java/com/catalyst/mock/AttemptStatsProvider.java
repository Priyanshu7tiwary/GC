package com.catalyst.mock;

import java.util.Map;
import java.util.UUID;

/**
 * Port the mock catalogue uses to show per-learner progress, implemented by the attempts module.
 * Keeps the catalogue independent of how attempts are stored.
 */
public interface AttemptStatsProvider {
    Map<String, AttemptStats> statsByMock(UUID userId);
}

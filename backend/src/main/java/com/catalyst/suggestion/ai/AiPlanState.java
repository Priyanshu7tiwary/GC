package com.catalyst.suggestion.ai;

import java.time.Instant;

/** The learner's latest AI plan (if any) and how many more they may generate this week. */
public record AiPlanState(boolean available, AiSuggestionResponse plan, Quota quota) {
    /** A rolling 7-day allowance. nextAvailableAt is set only once it is used up: when the oldest counted plan ages out. */
    public record Quota(int limit, int used, int remaining, Instant nextAvailableAt) {}
}

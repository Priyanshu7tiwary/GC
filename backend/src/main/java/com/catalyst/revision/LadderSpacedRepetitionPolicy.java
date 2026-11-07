package com.catalyst.revision;

import org.springframework.stereotype.Component;

/** A fixed interval ladder: 1 → 3 → 7 → 15 → 30 → 60 days. Hard resets, Good climbs one rung, Easy climbs two. */
@Component
class LadderSpacedRepetitionPolicy implements SpacedRepetitionPolicy {
    private static final int[] INTERVALS = {1, 3, 7, 15, 30, 60};

    @Override
    public int initialStage() {
        return 0;
    }

    @Override
    public int nextStage(int current, Rating rating) {
        int next = switch (rating) {
            case HARD -> 0;
            case GOOD -> current + 1;
            case EASY -> current + 2;
        };
        return Math.min(next, INTERVALS.length - 1);
    }

    @Override
    public int intervalDays(int stage) {
        return INTERVALS[Math.max(0, Math.min(stage, INTERVALS.length - 1))];
    }

    @Override
    public int retainedStage() {
        return 4;
    }
}

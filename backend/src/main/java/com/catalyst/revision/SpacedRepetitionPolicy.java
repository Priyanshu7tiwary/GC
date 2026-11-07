package com.catalyst.revision;

/** Decides how far apart revisions are spaced. Swap the implementation to change the algorithm (e.g. SM-2). */
public interface SpacedRepetitionPolicy {
    int initialStage();

    int nextStage(int currentStage, Rating rating);

    int intervalDays(int stage);

    /** Stage at or beyond which a topic counts as retained long-term. */
    int retainedStage();
}

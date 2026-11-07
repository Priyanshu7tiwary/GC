package com.catalyst.attempt.scoring;

/** Maps a score ratio (score ÷ max) to an estimated percentile, and back. */
public interface PercentileEstimator {
    double estimate(double scoreRatio);

    double requiredRatio(double percentile);
}

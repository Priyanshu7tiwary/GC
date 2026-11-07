package com.catalyst.attempt.scoring;

import org.springframework.stereotype.Component;

/** Rough logistic fit to recent CAT results (≈ 100/198 → 99 %ile). Replace with real score-vs-percentile data when available. */
@Component
class LogisticPercentileEstimator implements PercentileEstimator {
    private static final double MIDPOINT = 0.1, STEEPNESS = 12;

    @Override
    public double estimate(double ratio) {
        double p = 100 / (1 + Math.exp(-(ratio - MIDPOINT) * STEEPNESS));
        return Math.max(0, Math.min(99.99, p));
    }

    @Override
    public double requiredRatio(double percentile) {
        double p = Math.min(percentile, 99.95);
        return MIDPOINT - Math.log(100 / p - 1) / STEEPNESS;
    }
}

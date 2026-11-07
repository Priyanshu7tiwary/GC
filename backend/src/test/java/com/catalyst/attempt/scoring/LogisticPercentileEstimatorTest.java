package com.catalyst.attempt.scoring;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class LogisticPercentileEstimatorTest {
    private final LogisticPercentileEstimator model = new LogisticPercentileEstimator();

    @Test
    void roughlyHundredOutOf204IsNinetyNinth() {
        assertThat(model.estimate(100 / 204.0)).isBetween(98.5, 99.5);
    }

    @Test
    void inverseRoundTrips() {
        for (double p : new double[]{80, 90, 95, 99, 99.5}) {
            assertThat(model.estimate(model.requiredRatio(p))).isCloseTo(p, within(0.01));
        }
    }

    @Test
    void neverReportsAHundred() {
        assertThat(model.estimate(1.0)).isLessThan(100);
    }
}

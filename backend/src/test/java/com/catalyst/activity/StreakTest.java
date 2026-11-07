package com.catalyst.activity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StreakTest {
    private final LocalDate today = LocalDate.of(2026, 11, 7);

    @Test
    void countsConsecutiveDaysEndingToday() {
        var logs = Map.of(today, 30, today.minusDays(1), 20, today.minusDays(2), 10, today.minusDays(4), 50);
        assertThat(ActivityService.streak(logs, today)).isEqualTo(3);
    }

    @Test
    void noStudyYetTodayDoesNotBreakTheStreak() {
        var logs = Map.of(today.minusDays(1), 20, today.minusDays(2), 10);
        assertThat(ActivityService.streak(logs, today)).isEqualTo(2);
    }
}

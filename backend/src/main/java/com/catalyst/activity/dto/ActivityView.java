package com.catalyst.activity.dto;

import java.time.LocalDate;
import java.util.List;

public record ActivityView(int streakDays, int minutesLast7Days, int minutesPrevious7Days, List<Day> days) {
    public record Day(LocalDate day, int minutes) {}
}

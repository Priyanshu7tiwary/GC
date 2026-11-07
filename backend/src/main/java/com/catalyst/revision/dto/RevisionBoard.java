package com.catalyst.revision.dto;

import java.util.List;

public record RevisionBoard(Summary summary, List<Integer> intervals, List<ForecastDay> forecast, List<RevisionView> items) {
    public record Summary(int overdue, int dueToday, int upcomingThisWeek, int scheduled, int retained) {}

    public record ForecastDay(java.time.LocalDate day, int due) {}
}

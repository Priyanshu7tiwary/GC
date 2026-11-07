package com.catalyst.user;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** CAT is conventionally held on the last Sunday of November. */
@Component
class CatExamCalendar implements ExamCalendar {
    @Override
    public LocalDate examDate(int year) {
        return LocalDate.of(year, 11, 30).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    @Override
    public int upcomingYear(LocalDate today) {
        return examDate(today.getYear()).isBefore(today) ? today.getYear() + 1 : today.getYear();
    }
}

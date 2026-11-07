package com.catalyst.user;

import java.time.LocalDate;

/** Knows when a given year's exam is held. */
public interface ExamCalendar {
    LocalDate examDate(int year);

    /** The first exam year whose exam has not happened yet. */
    int upcomingYear(LocalDate today);
}

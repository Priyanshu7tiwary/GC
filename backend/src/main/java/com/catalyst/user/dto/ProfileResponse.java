package com.catalyst.user.dto;

import com.catalyst.user.User;

import java.time.LocalDate;
import java.util.UUID;

public record ProfileResponse(UUID id, String email, String name, double targetPercentile, int targetYear,
                              LocalDate examDate, long daysToExam, boolean hasPassword, boolean googleLinked) {
    public static ProfileResponse of(User u, LocalDate today) {
        return new ProfileResponse(u.getId(), u.getEmail(), u.getName(), u.getTargetPercentile(), u.getTargetYear(),
                u.getExamDate(), Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(today, u.getExamDate())),
                u.hasPassword(), u.getGoogleSubject() != null);
    }
}

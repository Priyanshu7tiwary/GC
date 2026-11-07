package com.catalyst.user;

import com.catalyst.common.error.NotFoundException;
import com.catalyst.user.dto.ProfileResponse;
import com.catalyst.user.dto.UpdateProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class ProfileService {
    private final UserRepository users;
    private final ExamCalendar calendar;
    private final Clock clock;

    public ProfileService(UserRepository users, ExamCalendar calendar, Clock clock) {
        this.users = users;
        this.calendar = calendar;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public User require(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("User not found."));
    }

    @Transactional(readOnly = true)
    public ProfileResponse get(UUID userId) {
        return ProfileResponse.of(require(userId), LocalDate.now(clock));
    }

    @Transactional
    public ProfileResponse update(UUID userId, UpdateProfileRequest req) {
        User user = require(userId);
        if (req.name() != null) user.rename(req.name().trim());
        if (req.targetPercentile() != null) user.setTarget(req.targetPercentile());
        if (req.examDate() != null) {
            user.scheduleExam(req.examDate().getYear(), req.examDate());
        } else if (req.targetYear() != null) {
            user.scheduleExam(req.targetYear(), calendar.examDate(req.targetYear()));
        }
        return ProfileResponse.of(user, LocalDate.now(clock));
    }
}

package com.catalyst.activity;

import com.catalyst.activity.dto.ActivityView;
import com.catalyst.events.StudySessionEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ActivityService {
    private final StudyLogRepository logs;
    private final Clock clock;

    public ActivityService(StudyLogRepository logs, Clock clock) {
        this.logs = logs;
        this.clock = clock;
    }

    @EventListener
    @Transactional
    public void onStudySession(StudySessionEvent e) {
        if (e.minutes() > 0) logs.addMinutes(e.userId(), LocalDate.now(clock), e.minutes());
    }

    /** Minutes per day for the last {@code days} days (oldest first), plus streak and weekly totals. */
    @Transactional(readOnly = true)
    public ActivityView view(UUID userId, int days) {
        LocalDate today = LocalDate.now(clock);
        int window = Math.max(days, 14);
        Map<LocalDate, Integer> byDay = logs.findByUserIdAndDayGreaterThanEqualOrderByDayAsc(userId, today.minusDays(window - 1)).stream()
                .collect(Collectors.toMap(StudyLog::getDay, StudyLog::getMinutes));

        List<ActivityView.Day> list = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            list.add(new ActivityView.Day(d, byDay.getOrDefault(d, 0)));
        }
        return new ActivityView(streak(byDay, today), sum(byDay, today, 0), sum(byDay, today, 7), list);
    }

    /** Consecutive study days ending today; an unlogged today does not break the streak. */
    static int streak(Map<LocalDate, Integer> byDay, LocalDate today) {
        LocalDate d = byDay.getOrDefault(today, 0) > 0 ? today : today.minusDays(1);
        int n = 0;
        while (byDay.getOrDefault(d, 0) > 0) {
            n++;
            d = d.minusDays(1);
        }
        return n;
    }

    private static int sum(Map<LocalDate, Integer> byDay, LocalDate today, int offset) {
        int total = 0;
        for (int i = 0; i < 7; i++) total += byDay.getOrDefault(today.minusDays(i + offset), 0);
        return total;
    }
}

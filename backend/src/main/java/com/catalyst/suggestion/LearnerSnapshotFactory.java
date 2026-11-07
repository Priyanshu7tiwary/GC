package com.catalyst.suggestion;

import com.catalyst.activity.ActivityService;
import com.catalyst.attempt.AttemptService;
import com.catalyst.attempt.dto.AttemptSummary;
import com.catalyst.attempt.scoring.PercentileEstimator;
import com.catalyst.coverage.CoverageService;
import com.catalyst.revision.RevisionService;
import com.catalyst.user.ProfileService;
import com.catalyst.user.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class LearnerSnapshotFactory {
    private final ProfileService profiles;
    private final AttemptService attempts;
    private final CoverageService coverage;
    private final RevisionService revisions;
    private final ActivityService activity;
    private final PercentileEstimator percentiles;
    private final Clock clock;

    public LearnerSnapshotFactory(ProfileService profiles, AttemptService attempts, CoverageService coverage,
                                  RevisionService revisions, ActivityService activity, PercentileEstimator percentiles, Clock clock) {
        this.profiles = profiles;
        this.attempts = attempts;
        this.coverage = coverage;
        this.revisions = revisions;
        this.activity = activity;
        this.percentiles = percentiles;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LearnerSnapshot build(UUID userId) {
        User u = profiles.require(userId);
        LocalDate today = LocalDate.now(clock);
        return new LearnerSnapshot(today, u.getTargetPercentile(), u.getTargetYear(),
                Math.max(0, ChronoUnit.DAYS.between(today, u.getExamDate())),
                percentiles.requiredRatio(u.getTargetPercentile()),
                attempts.list(userId).stream().filter(AttemptSummary::fullLength).toList(),
                coverage.topicsFor(userId), revisions.list(userId), activity.view(userId, 14));
    }
}

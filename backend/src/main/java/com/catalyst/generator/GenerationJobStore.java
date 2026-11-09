package com.catalyst.generator;

import com.catalyst.common.error.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/** Persists job progress; each call commits on its own so pollers see steps advance live. */
@Component
class GenerationJobStore {
    private final GenerationJobRepository jobs;
    private final Clock clock;

    GenerationJobStore(GenerationJobRepository jobs, Clock clock) {
        this.jobs = jobs;
        this.clock = clock;
    }

    @Transactional
    GenerationJob create(GenerationJob job) {
        return jobs.save(job);
    }

    /** A job that has not progressed for this long is treated as dead and no longer blocks the user. */
    static final java.time.Duration STALE_AFTER = java.time.Duration.ofMinutes(15);

    @Transactional(readOnly = true)
    boolean hasActiveJob(UUID userId) {
        return jobs.existsByUserIdAndStatusInAndUpdatedAtAfter(userId, java.util.List.of(JobStatus.QUEUED, JobStatus.RUNNING),
                clock.instant().minus(STALE_AFTER));
    }

    /** Jobs left queued or running by a previous server process can never finish; mark them failed. */
    @Transactional
    int failInterrupted() {
        var stuck = jobs.findByStatusIn(java.util.List.of(JobStatus.QUEUED, JobStatus.RUNNING));
        stuck.forEach(j -> j.failed(j.runningStep(), "Interrupted by a server restart. Please generate the mock again.", clock.instant()));
        return stuck.size();
    }

    @Transactional(readOnly = true)
    long startedSince(UUID userId, java.time.Instant since) {
        return jobs.countByUserIdAndCreatedAtAfter(userId, since);
    }

    @Transactional(readOnly = true)
    GenerationJob get(UUID id) {
        return jobs.findById(id).orElseThrow(() -> new NotFoundException("Generation job not found."));
    }

    @Transactional
    void stepStarted(UUID id, int index) {
        get(id).stepStarted(index, clock.instant());
    }

    @Transactional
    void stepFinished(UUID id, int index, String info) {
        get(id).stepFinished(index, info, clock.instant());
    }

    @Transactional
    void succeeded(UUID id, String mockId) {
        get(id).succeeded(mockId, clock.instant());
    }

    @Transactional
    void failed(UUID id, int index, String message) {
        get(id).failed(index, message, clock.instant());
    }
}

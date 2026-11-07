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

    @Transactional(readOnly = true)
    boolean hasActiveJob(UUID userId) {
        return jobs.existsByUserIdAndStatusIn(userId, java.util.List.of(JobStatus.QUEUED, JobStatus.RUNNING));
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

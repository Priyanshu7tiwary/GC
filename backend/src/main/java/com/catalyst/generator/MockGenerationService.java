package com.catalyst.generator;

import com.catalyst.common.error.ConflictException;
import com.catalyst.common.error.NotFoundException;
import com.catalyst.common.error.TooManyRequestsException;
import com.catalyst.generator.dto.GenerationJobView;
import com.catalyst.generator.dto.GenerationRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class MockGenerationService {
    private static final int HOURLY_LIMIT = 20;

    private final List<GenerationStep> steps;
    private final GenerationJobStore store;
    private final GenerationJobRunner runner;
    private final Clock clock;

    public MockGenerationService(List<GenerationStep> steps, GenerationJobStore store, GenerationJobRunner runner, Clock clock) {
        this.steps = steps;
        this.store = store;
        this.runner = runner;
        this.clock = clock;
    }

    /** Queues a generation job and starts it in the background. Not transactional: the job row commits before the runner reads it. */
    public GenerationJobView start(UUID userId, GenerationRequest request) {
        GenerationRequest normalized = request.normalized();
        if (store.hasActiveJob(userId)) {
            throw new ConflictException("A mock is already being generated. Wait for it to finish.");
        }
        if (store.startedSince(userId, clock.instant().minus(java.time.Duration.ofHours(1))) >= HOURLY_LIMIT) {
            throw new TooManyRequestsException("You can generate up to " + HOURLY_LIMIT + " mocks an hour. Try again later.");
        }
        List<StepState> plan = steps.stream()
                .map(s -> new StepState(s.id(), s.label(), s.detail(), StepState.Status.PENDING, null)).toList();
        GenerationJob job = store.create(new GenerationJob(userId, normalized, plan, clock.instant()));
        runner.run(job.getId());
        return GenerationJobView.of(job);
    }

    public GenerationJobView get(UUID userId, UUID jobId) {
        GenerationJob job = store.get(jobId);
        if (!job.getUserId().equals(userId)) throw new NotFoundException("Generation job not found.");
        return GenerationJobView.of(job);
    }
}

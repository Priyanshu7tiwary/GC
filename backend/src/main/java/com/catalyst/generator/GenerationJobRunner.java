package com.catalyst.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** Executes the pipeline in the background, recording each step's progress on the job. */
@Component
class GenerationJobRunner {
    private static final Logger log = LoggerFactory.getLogger(GenerationJobRunner.class);

    private final List<GenerationStep> steps;
    private final GenerationJobStore store;
    private final Duration stepPause;

    GenerationJobRunner(List<GenerationStep> steps, GenerationJobStore store,
                        @Value("${app.generator.step-pause:PT0.4S}") Duration stepPause) {
        this.steps = steps;
        this.store = store;
        this.stepPause = stepPause;
    }

    @Async
    public void run(UUID jobId) {
        GenerationJob job = store.get(jobId);
        GenerationContext ctx = new GenerationContext(job.getUserId(), job.getRequest());
        int i = -1;
        try {
            for (i = 0; i < steps.size(); i++) {
                store.stepStarted(jobId, i);
                pause();
                store.stepFinished(jobId, i, steps.get(i).execute(ctx));
            }
            store.succeeded(jobId, ctx.resultMockId());
        } catch (RuntimeException e) {
            log.warn("Mock generation {} failed at step {}", jobId, i, e);
            store.failed(jobId, i < steps.size() ? i : -1, e.getMessage() == null ? "Generation failed." : e.getMessage());
        }
    }

    private void pause() {
        if (stepPause.isZero()) return;
        try {
            Thread.sleep(stepPause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

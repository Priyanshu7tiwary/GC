package com.catalyst.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** On startup, fails jobs that a previous server process left queued or running (they ran in-memory and are gone). */
@Component
class GenerationJobRecovery {
    private static final Logger log = LoggerFactory.getLogger(GenerationJobRecovery.class);

    private final GenerationJobStore store;

    GenerationJobRecovery(GenerationJobStore store) {
        this.store = store;
    }

    @EventListener(ApplicationReadyEvent.class)
    void failInterruptedJobs() {
        int n = store.failInterrupted();
        if (n > 0) log.warn("Marked {} interrupted mock-generation job(s) as failed", n);
    }
}

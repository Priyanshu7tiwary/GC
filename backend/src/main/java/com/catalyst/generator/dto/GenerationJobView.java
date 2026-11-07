package com.catalyst.generator.dto;

import com.catalyst.generator.GenerationJob;
import com.catalyst.generator.JobStatus;
import com.catalyst.generator.StepState;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GenerationJobView(UUID id, JobStatus status, GenerationRequest request, List<StepState> steps,
                                String resultMockId, String error, Instant createdAt) {
    public static GenerationJobView of(GenerationJob j) {
        return new GenerationJobView(j.getId(), j.getStatus(), j.getRequest(), j.getSteps(), j.getResultMockId(),
                j.getError(), j.getCreatedAt());
    }
}

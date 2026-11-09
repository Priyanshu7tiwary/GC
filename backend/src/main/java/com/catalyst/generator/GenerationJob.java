package com.catalyst.generator;

import com.catalyst.generator.dto.GenerationRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "generation_jobs")
public class GenerationJob {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private GenerationRequest request;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<StepState> steps;
    @Column(name = "result_mock_id")
    private String resultMockId;
    private String error;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GenerationJob() {}

    public GenerationJob(UUID userId, GenerationRequest request, List<StepState> steps, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.request = request;
        this.steps = new ArrayList<>(steps);
        this.status = JobStatus.QUEUED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void stepStarted(int index, Instant now) {
        status = JobStatus.RUNNING;
        updateStep(index, StepState.Status.RUNNING, null, now);
    }

    void stepFinished(int index, String info, Instant now) {
        updateStep(index, StepState.Status.DONE, info, now);
    }

    void succeeded(String mockId, Instant now) {
        status = JobStatus.SUCCEEDED;
        resultMockId = mockId;
        updatedAt = now;
    }

    void failed(int index, String message, Instant now) {
        if (index >= 0) updateStep(index, StepState.Status.FAILED, message, now);
        status = JobStatus.FAILED;
        error = message;
        updatedAt = now;
    }

    private void updateStep(int index, StepState.Status s, String info, Instant now) {
        List<StepState> copy = new ArrayList<>(steps); // new list so Hibernate detects the JSON change
        copy.set(index, copy.get(index).with(s, info));
        steps = copy;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public JobStatus getStatus() { return status; }
    public GenerationRequest getRequest() { return request; }
    public List<StepState> getSteps() { return steps; }
    public String getResultMockId() { return resultMockId; }
    public String getError() { return error; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /** Index of the step that was running, or -1. */
    int runningStep() {
        for (int i = 0; i < steps.size(); i++) if (steps.get(i).status() == StepState.Status.RUNNING) return i;
        return -1;
    }
}

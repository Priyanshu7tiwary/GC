package com.catalyst.generator;

/** Progress of one pipeline step, as stored on the job and shown in the UI's workflow list. */
public record StepState(String id, String label, String detail, Status status, String info) {
    public enum Status { PENDING, RUNNING, DONE, FAILED }

    public StepState with(Status s, String newInfo) {
        return new StepState(id, label, detail, s, newInfo == null ? info : newInfo);
    }
}

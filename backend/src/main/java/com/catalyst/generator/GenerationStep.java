package com.catalyst.generator;

/**
 * One stage of the mock-generation workflow. Steps run in {@link org.springframework.core.annotation.Order} order;
 * adding a stage means adding a component, not editing the pipeline.
 */
public interface GenerationStep {
    String id();

    String label();

    String detail();

    /** Does the work and returns a short, user-facing result line (e.g. "6 weak topics found"). */
    String execute(GenerationContext ctx);
}

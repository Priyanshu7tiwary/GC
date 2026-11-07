package com.catalyst.coverage.dto;

import com.catalyst.coverage.CoverageStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Send status, practiced (absolute count), or practicedDelta (e.g. +10) — any combination. */
public record UpdateCoverageRequest(CoverageStatus status, @Min(0) @Max(100000) Integer practiced, Integer practicedDelta) {}

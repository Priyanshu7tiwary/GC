package com.catalyst.revision.dto;

import com.catalyst.revision.Rating;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** requestId: optional client-generated id; sending the same id again (a retry) never applies the review twice. */
public record ReviewRequest(@NotNull Rating rating, @Pattern(regexp = "[A-Za-z0-9-]{8,64}") String requestId) {}

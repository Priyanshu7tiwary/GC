package com.catalyst.revision.dto;

import com.catalyst.revision.Rating;
import jakarta.validation.constraints.NotNull;

public record ReviewRequest(@NotNull Rating rating) {}

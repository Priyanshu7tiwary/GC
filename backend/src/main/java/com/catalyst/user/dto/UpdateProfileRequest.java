package com.catalyst.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Every field is optional; only the ones sent are changed. Sending targetYear without examDate uses that year's exam day. */
public record UpdateProfileRequest(
        @Size(min = 1, max = 120) String name,
        @DecimalMin("50") @DecimalMax("100") Double targetPercentile,
        @Min(2020) @Max(2100) Integer targetYear,
        LocalDate examDate) {}

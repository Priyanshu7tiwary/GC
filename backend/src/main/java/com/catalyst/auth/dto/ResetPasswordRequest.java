package com.catalyst.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ResetPasswordRequest(
        @NotNull UUID challengeId,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Enter the 6-digit code.") String code,
        @NotBlank @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters.") String password) {}

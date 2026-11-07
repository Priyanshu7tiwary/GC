package com.catalyst.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The ID token ("credential") returned to the browser by Google Identity Services. */
public record GoogleLoginRequest(@NotBlank @Size(max = 4096) String credential) {}

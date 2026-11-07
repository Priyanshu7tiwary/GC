package com.catalyst.generator;

import com.catalyst.generator.dto.GenerationJobView;
import com.catalyst.generator.dto.GenerationRequest;
import com.catalyst.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
class GeneratorController {
    private final MockGenerationService generator;

    GeneratorController(MockGenerationService generator) {
        this.generator = generator;
    }

    /** Starts the AI mock workflow. Poll the returned job until status is SUCCEEDED, then open resultMockId. */
    @PostMapping("/api/mocks/custom")
    @ResponseStatus(HttpStatus.ACCEPTED)
    GenerationJobView generate(@AuthenticationPrincipal AuthenticatedUser me, @Valid @RequestBody GenerationRequest req) {
        return generator.start(me.id(), req);
    }

    @GetMapping("/api/generation-jobs/{id}")
    GenerationJobView job(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID id) {
        return generator.get(me.id(), id);
    }
}

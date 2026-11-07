package com.catalyst.attempt;

import com.catalyst.attempt.dto.AttemptDetail;
import com.catalyst.attempt.dto.AttemptSummary;
import com.catalyst.attempt.dto.SubmitAttemptRequest;
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

import java.util.List;
import java.util.UUID;

@RestController
class AttemptController {
    private final AttemptService attempts;

    AttemptController(AttemptService attempts) {
        this.attempts = attempts;
    }

    @PostMapping("/api/mocks/{mockId}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    AttemptDetail submit(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String mockId,
                         @Valid @RequestBody SubmitAttemptRequest req) {
        return attempts.submit(me.id(), mockId, req);
    }

    @GetMapping("/api/attempts")
    List<AttemptSummary> list(@AuthenticationPrincipal AuthenticatedUser me) {
        return attempts.list(me.id());
    }

    @GetMapping("/api/attempts/{id}")
    AttemptDetail get(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID id) {
        return attempts.get(me.id(), id);
    }
}

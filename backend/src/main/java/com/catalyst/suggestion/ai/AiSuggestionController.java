package com.catalyst.suggestion.ai;

import com.catalyst.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AiSuggestionController {
    private final AiSuggestionService service;

    AiSuggestionController(AiSuggestionService service) {
        this.service = service;
    }

    /** The latest AI plan (if any) and how many more this learner may generate this week. Costs no allowance. */
    @GetMapping("/api/suggestions/ai")
    AiPlanState state(@AuthenticationPrincipal AuthenticatedUser me) {
        return service.state(me.id());
    }

    /** Generates a new AI plan: topics to cover and tests to attempt, with links. Limited per learner per week. */
    @PostMapping("/api/suggestions/ai")
    AiPlanState suggest(@AuthenticationPrincipal AuthenticatedUser me, @Valid @RequestBody AiSuggestionRequest req) {
        return service.suggest(me.id(), req);
    }
}

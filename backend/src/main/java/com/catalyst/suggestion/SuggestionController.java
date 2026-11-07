package com.catalyst.suggestion;

import com.catalyst.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class SuggestionController {
    private final SuggestionService suggestions;

    SuggestionController(SuggestionService suggestions) {
        this.suggestions = suggestions;
    }

    /** Ranked, actionable advice built from the learner's mocks, coverage, revisions, activity and targets. */
    @GetMapping("/api/suggestions")
    List<Suggestion> get(@AuthenticationPrincipal AuthenticatedUser me) {
        return suggestions.suggestionsFor(me.id());
    }
}

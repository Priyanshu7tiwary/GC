package com.catalyst.suggestion.rules;

import com.catalyst.suggestion.LearnerSnapshot;
import com.catalyst.suggestion.Priority;
import com.catalyst.suggestion.Suggestion;
import com.catalyst.suggestion.SuggestionRule;
import org.springframework.stereotype.Component;

import java.util.Optional;
import com.catalyst.coverage.CoverageStatus;
import com.catalyst.coverage.dto.CoverageView.TopicCoverage;
import java.util.List;

@Component
class WeakAreaMockRule implements SuggestionRule {
    private static final int LOW_PRACTICE = 30;

    @Override
    public Optional<Suggestion> evaluate(LearnerSnapshot s) {
        List<TopicCoverage> weak = s.coverage().stream()
                .filter(t -> t.status() == CoverageStatus.IN_PROGRESS && t.practiced() < LOW_PRACTICE).toList();
        if (weak.size() < 2) return Optional.empty();
        return Optional.of(new Suggestion("custom", "Practice", Priority.LOW, "Generate a weak-area mock",
                weak.size() + " in-progress topics have under " + LOW_PRACTICE + " practice questions (e.g. "
                        + weak.get(0).name() + ", " + weak.get(1).name() + "). A custom mock weighted toward them builds volume fast.",
                weak.size() + " topics", new Suggestion.Action("Open generator", "/mocks?tab=custom")));
    }
}

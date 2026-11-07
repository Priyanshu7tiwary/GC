package com.catalyst.suggestion.rules;

import com.catalyst.suggestion.LearnerSnapshot;
import com.catalyst.suggestion.Priority;
import com.catalyst.suggestion.Suggestion;
import com.catalyst.suggestion.SuggestionRule;
import org.springframework.stereotype.Component;

import java.util.Optional;
import com.catalyst.attempt.dto.AttemptSummary;
import java.util.List;

@Component
class ScoreTrendRule implements SuggestionRule {
    @Override
    public Optional<Suggestion> evaluate(LearnerSnapshot s) {
        List<AttemptSummary> fulls = s.fullMocks();
        if (fulls.size() < 3) return Optional.empty();
        double first = pct(fulls.get(fulls.size() - 3)), last = pct(fulls.getLast());
        double slope = last - first;
        if (slope >= -2) return Optional.empty();
        return Optional.of(new Suggestion("trend-down", "Performance", Priority.HIGH,
                "Your last mocks are trending down",
                "Percentile dropped " + String.format("%.1f", -slope) + " points over your last 3 full mocks. Pause new mocks for two days and analyse the last two attempts question by question.",
                String.format("%.1f", slope) + " %ile",
                new Suggestion.Action("Review last mock", "/mocks/result/" + fulls.getLast().id())));
    }

    private static double pct(AttemptSummary a) {
        return a.percentile() == null ? 0 : a.percentile();
    }
}

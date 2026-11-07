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
import java.util.stream.Collectors;

@Component
class HighYieldTopicsRule implements SuggestionRule {
    @Override
    public Optional<Suggestion> evaluate(LearnerSnapshot s) {
        List<TopicCoverage> untouched = s.coverage().stream()
                .filter(t -> t.highYield() && t.status() == CoverageStatus.NOT_STARTED).toList();
        if (untouched.isEmpty()) return Optional.empty();
        String names = untouched.stream().limit(3).map(TopicCoverage::name).collect(Collectors.joining(", "));
        String more = untouched.size() > 3 ? " and " + (untouched.size() - 3) + " more" : "";
        return Optional.of(new Suggestion("high-yield", "Coverage", untouched.size() >= 3 ? Priority.HIGH : Priority.MEDIUM,
                "Start " + untouched.size() + " high-yield topic" + (untouched.size() > 1 ? "s" : ""),
                names + more + ". These chapters carry the most marks in recent papers and you haven't begun them.",
                untouched.getFirst().section().name(), new Suggestion.Action("Plan them", "/coverage")));
    }
}

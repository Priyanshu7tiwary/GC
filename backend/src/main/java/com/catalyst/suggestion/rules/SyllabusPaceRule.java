package com.catalyst.suggestion.rules;

import com.catalyst.suggestion.LearnerSnapshot;
import com.catalyst.suggestion.Priority;
import com.catalyst.suggestion.Suggestion;
import com.catalyst.suggestion.SuggestionRule;
import org.springframework.stereotype.Component;

import java.util.Optional;
import com.catalyst.common.domain.Section;
import com.catalyst.coverage.CoverageService;
import java.util.Arrays;
import java.util.Comparator;

@Component
class SyllabusPaceRule implements SuggestionRule {
    @Override
    public Optional<Suggestion> evaluate(LearnerSnapshot s) {
        var overall = CoverageService.stats(s.coverage());
        int remaining = overall.total() - overall.completed();
        if (remaining == 0) return Optional.empty();
        Section lowest = Arrays.stream(Section.values()).min(Comparator.comparingInt(sec ->
                CoverageService.stats(s.coverage().stream().filter(t -> t.section() == sec).toList()).percent())).orElseThrow();
        int lowestPct = CoverageService.stats(s.coverage().stream().filter(t -> t.section() == lowest).toList()).percent();
        double perWeek = s.daysLeft() == 0 ? remaining : remaining * 7.0 / s.daysLeft();
        return Optional.of(new Suggestion("coverage", "Coverage", perWeek > 4 ? Priority.HIGH : Priority.MEDIUM,
                remaining + " topics left. " + lowest + " is furthest behind.",
                lowest + " is at " + lowestPct + "% coverage. To finish before CAT " + s.targetYear() + " you need about "
                        + (int) Math.ceil(perWeek) + " topics a week" + (perWeek > 4 ? ". Consider dropping low-yield topics and protecting high-yield ones." : "."),
                overall.percent() + "% covered", new Suggestion.Action("Open coverage", "/coverage")));
    }
}

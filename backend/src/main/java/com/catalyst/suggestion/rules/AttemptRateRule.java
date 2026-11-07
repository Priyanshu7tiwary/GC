package com.catalyst.suggestion.rules;

import com.catalyst.suggestion.LearnerSnapshot;
import com.catalyst.suggestion.Priority;
import com.catalyst.suggestion.Suggestion;
import com.catalyst.suggestion.SuggestionRule;
import org.springframework.stereotype.Component;

import java.util.Optional;
import com.catalyst.attempt.scoring.SectionResult;
import com.catalyst.common.domain.Section;
import java.util.Arrays;
import java.util.Comparator;

/** Flags a section where accuracy is fine but too few questions are attempted. */
@Component
class AttemptRateRule implements SuggestionRule {
    @Override
    public Optional<Suggestion> evaluate(LearnerSnapshot s) {
        if (s.recentFullMocks().isEmpty()) return Optional.empty();
        Section low = Arrays.stream(Section.values()).min(Comparator.comparingDouble(sec -> rate(s, sec))).orElseThrow();
        double rate = rate(s, low) * 100, acc = s.recentAverage(low, SectionResult::accuracy) * 100;
        if (rate >= 55 || acc < 70) return Optional.empty();
        return Optional.of(new Suggestion("attempt-rate", "Mocks", Priority.MEDIUM,
                "Attempt more in " + low,
                "You attempt only " + Math.round(rate) + "% of " + low + " questions, at " + Math.round(acc)
                        + "% accuracy. That accuracy supports attempting more. Spend the first 5 minutes picking the easiest questions.",
                Math.round(rate) + "% attempted",
                new Suggestion.Action("Practise selection", "/mocks?tab=sectional&sec=" + low)));
    }

    private static double rate(LearnerSnapshot s, Section sec) {
        double attempted = s.recentAverage(sec, SectionResult::attempted);
        double total = s.recentAverage(sec, r -> r.attempted() + r.unattempted());
        return total == 0 ? 1 : attempted / total;
    }
}

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

@Component
class SectionAccuracyRule implements SuggestionRule {
    @Override
    public Optional<Suggestion> evaluate(LearnerSnapshot s) {
        if (s.recentFullMocks().isEmpty()) return Optional.empty();
        Section worst = Arrays.stream(Section.values())
                .min(Comparator.comparingDouble(sec -> s.recentAverage(sec, SectionResult::accuracy))).orElseThrow();
        double acc = s.recentAverage(worst, SectionResult::accuracy) * 100;
        if (acc >= 75) return Optional.empty();
        double neg = s.recentAverage(worst, r -> r.wrong());
        return Optional.of(new Suggestion("accuracy", "Mocks", acc < 65 ? Priority.HIGH : Priority.MEDIUM,
                "Fix accuracy in " + worst,
                "Your " + worst + " accuracy over the last " + s.recentFullMocks().size() + " mocks is " + Math.round(acc)
                        + "%, costing about " + String.format("%.1f", neg) + " negative marks per mock. Do a timed sectional and attempt only what you can verify.",
                Math.round(acc) + "% accuracy",
                new Suggestion.Action(worst + " sectionals", "/mocks?tab=sectional&sec=" + worst)));
    }
}

package com.catalyst.suggestion.rules;

import com.catalyst.suggestion.LearnerSnapshot;
import com.catalyst.suggestion.Priority;
import com.catalyst.suggestion.Suggestion;
import com.catalyst.suggestion.SuggestionRule;
import org.springframework.stereotype.Component;

import java.util.Optional;
import com.catalyst.common.domain.Section;
import java.util.Arrays;

/** Converts the target percentile into a score and shows the gap, section by section. */
@Component
class TargetGapRule implements SuggestionRule {
    @Override
    public Optional<Suggestion> evaluate(LearnerSnapshot s) {
        if (s.recentFullMocks().isEmpty()) return Optional.empty();
        int fullMax = Arrays.stream(Section.values()).mapToInt(Section::standardMaxMarks).sum();
        int need = (int) Math.round(s.requiredScoreRatio() * fullMax);
        double current = 0;
        Section worst = null;
        double worstGap = Double.NEGATIVE_INFINITY;
        StringBuilder split = new StringBuilder();
        for (Section sec : Section.values()) {
            int target = (int) Math.round((double) need * sec.standardMaxMarks() / fullMax);
            // scale to the standard paper so a short mock compares fairly with the target
            double avg = s.recentAverage(sec, r -> r.maxScore() == 0 ? 0 : (double) r.score() * sec.standardMaxMarks() / r.maxScore());
            current += avg;
            if (target - avg > worstGap) { worstGap = target - avg; worst = sec; }
            split.append(split.isEmpty() ? "" : ", ").append(sec).append(' ').append(target);
        }
        double gap = need - current;
        long weeks = Math.max(1, s.daysLeft() / 7);
        String pct = Rules.fmt(s.targetPercentile());
        if (gap <= 0) {
            return Optional.of(new Suggestion("target", "Target", Priority.LOW,
                    "You're scoring at " + pct + "+ level. Hold it.",
                    "Your recent average (" + Math.round(current) + ") already clears the ~" + need + " needed. Keep accuracy high and revision current.",
                    "on target", new Suggestion.Action("See analytics", "/analytics")));
        }
        Priority p = gap / weeks > 3 ? Priority.HIGH : Priority.MEDIUM;
        return Optional.of(new Suggestion("target", "Target", p,
                pct + " %ile in CAT " + s.targetYear() + " needs ~" + need + "/" + fullMax,
                "You average " + Math.round(current) + ", a gap of " + Math.round(gap) + " marks (about "
                        + String.format("%.1f", gap / weeks) + " marks a week over " + weeks + " weeks). Target split: " + split
                        + ". Biggest gap is " + worst + ".",
                "gap " + Math.round(gap) + " marks",
                new Suggestion.Action(worst + " sectionals", "/mocks?tab=sectional&sec=" + worst)));
    }
}

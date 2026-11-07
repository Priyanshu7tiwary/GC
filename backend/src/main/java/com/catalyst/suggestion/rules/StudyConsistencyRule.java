package com.catalyst.suggestion.rules;

import com.catalyst.suggestion.LearnerSnapshot;
import com.catalyst.suggestion.Priority;
import com.catalyst.suggestion.Suggestion;
import com.catalyst.suggestion.SuggestionRule;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class StudyConsistencyRule implements SuggestionRule {
    @Override
    public Optional<Suggestion> evaluate(LearnerSnapshot s) {
        int now = s.activity().minutesLast7Days(), before = s.activity().minutesPrevious7Days();
        if (before > 0 && now < before * 0.75) {
            return Optional.of(new Suggestion("hours", "Habits", Priority.MEDIUM, "Study time is down this week",
                    Rules.hours(now) + " in the last 7 days vs " + Rules.hours(before) + " the week before. Block a fixed daily slot. Consistency matters more than long weekend sessions.",
                    Math.round((1 - (double) now / before) * 100) + "% less", new Suggestion.Action("View dashboard", "/")));
        }
        int streak = s.activity().streakDays();
        if (streak >= 7) {
            return Optional.of(new Suggestion("streak", "Habits", Priority.LOW, streak + "-day streak. Keep it going.",
                    "You've studied " + Rules.hours(now) + " this week. Use today's session for one revision block and one timed sectional.",
                    streak + " days", new Suggestion.Action("Revise now", "/revision")));
        }
        return Optional.empty();
    }
}

package com.catalyst.suggestion.rules;

import com.catalyst.suggestion.LearnerSnapshot;
import com.catalyst.suggestion.Priority;
import com.catalyst.suggestion.Suggestion;
import com.catalyst.suggestion.SuggestionRule;
import org.springframework.stereotype.Component;

import java.util.Optional;
import com.catalyst.attempt.dto.AttemptSummary;
import java.time.temporal.ChronoUnit;

/** How often to sit full mocks depends on how close the exam is. */
@Component
class MockCadenceRule implements SuggestionRule {
    @Override
    public Optional<Suggestion> evaluate(LearnerSnapshot s) {
        Long since = s.fullMocks().isEmpty() ? null : ChronoUnit.DAYS.between(
                s.fullMocks().getLast().submittedAt().atZone(com.catalyst.common.config.TimeConfig.EXAM_ZONE).toLocalDate(), s.today());
        boolean close = s.daysLeft() <= 30;
        int gap = close ? 4 : s.longRunway() ? 21 : 7;
        if (since != null && since <= gap) return Optional.empty();
        String body = close ? "With " + s.daysLeft() + " days to CAT, aim for 2–3 full mocks a week, each followed by a full analysis session."
                : s.longRunway() ? "CAT " + s.targetYear() + " is " + Math.round(s.daysLeft() / 30.0) + " months away. One full mock every 2–3 weeks is enough to benchmark while you build fundamentals."
                : "One full mock a week keeps your stamina and section-switching sharp.";
        return Optional.of(new Suggestion("cadence", "Mocks", close ? Priority.HIGH : s.longRunway() ? Priority.LOW : Priority.MEDIUM,
                since == null ? "Take your first full mock" : "No full mock in " + since + " days", body,
                since == null ? "0 mocks" : "last: " + since + "d ago", new Suggestion.Action("Pick a paper", "/mocks?tab=pyq")));
    }
}

package com.catalyst.suggestion.rules;

import com.catalyst.suggestion.LearnerSnapshot;
import com.catalyst.suggestion.Priority;
import com.catalyst.suggestion.Suggestion;
import com.catalyst.suggestion.SuggestionRule;
import org.springframework.stereotype.Component;

import java.util.Optional;
import com.catalyst.revision.dto.RevisionView;
import com.catalyst.revision.dto.RevisionView.Bucket;
import java.util.List;

@Component
class RevisionBacklogRule implements SuggestionRule {
    private static final int MINUTES_EACH = 15;

    @Override
    public Optional<Suggestion> evaluate(LearnerSnapshot s) {
        List<RevisionView> overdue = s.revisions().stream().filter(r -> r.bucket() == Bucket.OVERDUE).toList();
        long dueToday = s.revisions().stream().filter(r -> r.bucket() == Bucket.DUE_TODAY).count();
        long total = overdue.size() + dueToday;
        if (total == 0) return Optional.empty();
        String oldest = overdue.isEmpty() ? "" : ". The oldest is \"" + overdue.getFirst().topicName() + "\", "
                + (-overdue.getFirst().daysUntilDue()) + " days late";
        return Optional.of(new Suggestion("revision", "Revision", overdue.size() >= 5 ? Priority.HIGH : Priority.MEDIUM,
                "Clear " + total + " pending revisions",
                overdue.size() + " overdue" + oldest + ". Topics left past their interval are the ones you forget fastest.",
                "≈ " + total * MINUTES_EACH + " min", new Suggestion.Action("Open revision", "/revision")));
    }
}

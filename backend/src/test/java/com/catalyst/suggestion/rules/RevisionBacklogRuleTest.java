package com.catalyst.suggestion.rules;

import com.catalyst.activity.dto.ActivityView;
import com.catalyst.common.domain.Section;
import com.catalyst.revision.dto.RevisionView;
import com.catalyst.revision.dto.RevisionView.Bucket;
import com.catalyst.suggestion.LearnerSnapshot;
import com.catalyst.suggestion.Priority;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RevisionBacklogRuleTest {
    private final LocalDate today = LocalDate.of(2026, 11, 7);
    private final RevisionBacklogRule rule = new RevisionBacklogRule();

    @Test
    void silentWhenNothingIsDue() {
        assertThat(rule.evaluate(snapshot(List.of(item(3, Bucket.UPCOMING))))).isEmpty();
    }

    @Test
    void highPriorityAtFiveOrMoreOverdue() {
        var items = IntStream.range(0, 5).mapToObj(i -> item(-(i + 1), Bucket.OVERDUE)).toList();
        var s = rule.evaluate(snapshot(items)).orElseThrow();
        assertThat(s.priority()).isEqualTo(Priority.HIGH);
        assertThat(s.title()).isEqualTo("Clear 5 pending revisions");
        assertThat(s.action().route()).isEqualTo("/revision");
    }

    private RevisionView item(int delta, Bucket b) {
        return new RevisionView("t" + delta, "Topic " + delta, Section.QA, "Algebra", 0, 1, today, today.plusDays(delta), delta, 1, b);
    }

    private LearnerSnapshot snapshot(List<RevisionView> items) {
        return new LearnerSnapshot(today, 99, 2026, 22, 0.48, List.of(), List.of(), items,
                new ActivityView(0, 0, 0, List.of()));
    }
}

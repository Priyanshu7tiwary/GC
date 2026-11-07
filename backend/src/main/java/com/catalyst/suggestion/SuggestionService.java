package com.catalyst.suggestion;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SuggestionService {
    private final List<SuggestionRule> rules;
    private final LearnerSnapshotFactory snapshots;

    public SuggestionService(List<SuggestionRule> rules, LearnerSnapshotFactory snapshots) {
        this.rules = rules;
        this.snapshots = snapshots;
    }

    public List<Suggestion> suggestionsFor(UUID userId) {
        LearnerSnapshot s = snapshots.build(userId);
        return rules.stream().map(r -> r.evaluate(s)).flatMap(Optional::stream)
                .sorted(Comparator.comparing(Suggestion::priority))
                .toList();
    }
}

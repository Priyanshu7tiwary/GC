package com.catalyst.suggestion;

import java.util.Optional;

/** A single, independent heuristic. Register a new one by adding a component; nothing else changes. */
public interface SuggestionRule {
    Optional<Suggestion> evaluate(LearnerSnapshot snapshot);
}

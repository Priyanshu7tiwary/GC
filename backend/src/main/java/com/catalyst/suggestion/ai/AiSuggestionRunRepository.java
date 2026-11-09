package com.catalyst.suggestion.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiSuggestionRunRepository extends JpaRepository<AiSuggestionRun, Long> {
    /** This learner's runs since a moment that count towards the limit, oldest first. */
    List<AiSuggestionRun> findByUserIdAndCountedTrueAndCreatedAtAfterOrderByCreatedAtAsc(UUID userId, Instant since);

    Optional<AiSuggestionRun> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}

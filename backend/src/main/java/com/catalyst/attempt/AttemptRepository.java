package com.catalyst.attempt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttemptRepository extends JpaRepository<Attempt, UUID> {
    List<Attempt> findByUserIdOrderBySubmittedAtAsc(UUID userId);

    Optional<Attempt> findByIdAndUserId(UUID id, UUID userId);
}

package com.catalyst.generator;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public interface GenerationJobRepository extends JpaRepository<GenerationJob, UUID> {
    boolean existsByUserIdAndStatusIn(UUID userId, Collection<JobStatus> statuses);

    boolean existsByUserIdAndStatusInAndUpdatedAtAfter(UUID userId, Collection<JobStatus> statuses, Instant since);

    java.util.List<GenerationJob> findByStatusIn(Collection<JobStatus> statuses);

    long countByUserIdAndCreatedAtAfter(UUID userId, Instant since);
}

package com.catalyst.revision;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RevisionEventRepository extends JpaRepository<RevisionEvent, Long> {
    List<RevisionEvent> findByUserIdAndTopicIdOrderByCreatedAtDesc(UUID userId, String topicId);

    boolean existsByUserIdAndRequestId(UUID userId, String requestId);
}

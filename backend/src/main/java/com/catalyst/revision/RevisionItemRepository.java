package com.catalyst.revision;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RevisionItemRepository extends JpaRepository<RevisionItem, Long> {
    List<RevisionItem> findByUserIdOrderByNextDueOnAsc(UUID userId);

    Optional<RevisionItem> findByUserIdAndTopicId(UUID userId, String topicId);

    void deleteByUserIdAndTopicId(UUID userId, String topicId);
}

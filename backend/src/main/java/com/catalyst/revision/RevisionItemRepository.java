package com.catalyst.revision;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RevisionItemRepository extends JpaRepository<RevisionItem, Long> {
    List<RevisionItem> findByUserIdOrderByNextDueOnAsc(UUID userId);

    Optional<RevisionItem> findByUserIdAndTopicId(UUID userId, String topicId);

    /** Row-locked read, so two simultaneous reviews of the same topic are applied one after the other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from RevisionItem i where i.userId = :userId and i.topicId = :topicId")
    Optional<RevisionItem> lockByUserIdAndTopicId(@Param("userId") UUID userId, @Param("topicId") String topicId);
}

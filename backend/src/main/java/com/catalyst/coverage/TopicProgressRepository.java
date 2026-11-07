package com.catalyst.coverage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicProgressRepository extends JpaRepository<TopicProgress, Long> {
    List<TopicProgress> findByUserId(UUID userId);

    Optional<TopicProgress> findByUserIdAndTopicId(UUID userId, String topicId);
}

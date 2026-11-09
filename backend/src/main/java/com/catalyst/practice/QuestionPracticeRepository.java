package com.catalyst.practice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionPracticeRepository extends JpaRepository<QuestionPractice, Long> {
    Optional<QuestionPractice> findByUserIdAndQuestionId(UUID userId, String questionId);

    List<QuestionPractice> findByUserIdAndQuestionIdIn(UUID userId, Collection<String> questionIds);

    /** Per topic: [topicId, solved, marked] for this learner. */
    @Query("""
            select q.topicId, sum(case when p.solved then 1 else 0 end), sum(case when p.marked then 1 else 0 end)
            from QuestionPractice p, Question q
            where p.questionId = q.id and p.userId = :user and (p.solved or p.marked) and q.topicId is not null
            group by q.topicId""")
    List<Object[]> countsByTopic(@Param("user") UUID userId);

    /** Per topic: [topicId, questions, keyed questions]. */
    @Query("select q.topicId, count(q), count(q.answer) from Question q where q.topicId is not null group by q.topicId")
    List<Object[]> bankSizes();

}

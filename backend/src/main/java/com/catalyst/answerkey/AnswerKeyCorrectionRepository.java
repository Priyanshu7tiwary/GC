package com.catalyst.answerkey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AnswerKeyCorrectionRepository extends JpaRepository<AnswerKeyCorrection, UUID> {
    /** Ids of the questions in one previous-year paper whose key an admin has changed. */
    @Query("select distinct c.questionId from AnswerKeyCorrection c, com.catalyst.question.Question q "
            + "where q.id = c.questionId and q.paperId = :paperId")
    List<String> correctedQuestionIds(@Param("paperId") String paperId);
}

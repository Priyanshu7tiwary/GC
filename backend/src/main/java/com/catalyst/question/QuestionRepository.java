package com.catalyst.question;

import com.catalyst.common.domain.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, String> {
    /** The original practice bank: questions that are not part of a previous-year paper. */
    List<Question> findBySectionInAndPaperIdIsNullOrderByPositionAsc(Collection<Section> sections);

    List<Question> findByIdIn(Collection<String> ids);

    /** One previous-year paper's questions, in paper order. */
    List<Question> findByPaperIdOrderByPositionAsc(String paperId);

    /** Everything a generated mock may draw from in a section: every question with an answer key. */
    List<Question> findBySectionAndAnswerIsNotNull(Section section);

    @Query("select coalesce(max(q.position), 0) from Question q")
    int maxPosition();

    @Query("select q.section, q.origin, count(q) from Question q where q.answer is not null group by q.section, q.origin")
    List<Object[]> countKeyedBySectionAndOrigin();

    boolean existsByPromptAndSection(String prompt, Section section);

    /** Per topic bank (null topic = unassigned) and origin: question count and keyed count. */
    @Query("select q.section, q.topicId, q.origin, count(q), count(q.answer) from Question q group by q.section, q.topicId, q.origin")
    List<Object[]> countByTopicAndOrigin();

    org.springframework.data.domain.Page<Question> findByTopicIdOrderByPositionAsc(String topicId, org.springframework.data.domain.Pageable page);

    org.springframework.data.domain.Page<Question> findBySectionAndTopicIdIsNullOrderByPositionAsc(Section section, org.springframework.data.domain.Pageable page);

    /** A topic's questions, filtered by this learner's state. filter: all | unsolved | solved | marked. */
    @Query(value = """
            select q from Question q
            where q.topicId = :topic and (
              :filter = 'all'
              or (:filter = 'solved' and exists (select 1 from com.catalyst.practice.QuestionPractice p where p.questionId = q.id and p.userId = :user and p.solved = true))
              or (:filter = 'marked' and exists (select 1 from com.catalyst.practice.QuestionPractice p where p.questionId = q.id and p.userId = :user and p.marked = true))
              or (:filter = 'unsolved' and not exists (select 1 from com.catalyst.practice.QuestionPractice p where p.questionId = q.id and p.userId = :user and p.solved = true)))
            order by case when q.paperId is null then 1 else 0 end, q.paperId desc, q.questionNumber, q.position""",
            countQuery = """
            select count(q) from Question q
            where q.topicId = :topic and (
              :filter = 'all'
              or (:filter = 'solved' and exists (select 1 from com.catalyst.practice.QuestionPractice p where p.questionId = q.id and p.userId = :user and p.solved = true))
              or (:filter = 'marked' and exists (select 1 from com.catalyst.practice.QuestionPractice p where p.questionId = q.id and p.userId = :user and p.marked = true))
              or (:filter = 'unsolved' and not exists (select 1 from com.catalyst.practice.QuestionPractice p where p.questionId = q.id and p.userId = :user and p.solved = true)))""")
    org.springframework.data.domain.Page<Question> topicQuestions(@Param("user") UUID userId, @Param("topic") String topicId, @Param("filter") String filter, org.springframework.data.domain.Pageable page);
}

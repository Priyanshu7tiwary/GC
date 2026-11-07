package com.catalyst.question;

import com.catalyst.common.domain.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, String> {
    List<Question> findBySectionInOrderByPositionAsc(Collection<Section> sections);

    List<Question> findByIdIn(Collection<String> ids);
}

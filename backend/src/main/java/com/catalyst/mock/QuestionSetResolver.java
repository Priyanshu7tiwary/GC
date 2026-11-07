package com.catalyst.mock;

import com.catalyst.common.domain.Section;
import com.catalyst.question.Question;
import com.catalyst.question.QuestionRepository;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Turns a mock into its ordered questions, grouped by section in the mock's section order. */
@Component
public class QuestionSetResolver {
    private final QuestionRepository questions;

    public QuestionSetResolver(QuestionRepository questions) {
        this.questions = questions;
    }

    public Map<Section, List<Question>> resolve(Mock mock) {
        List<Question> list;
        if (mock.getQuestionIds() != null) {
            Map<String, Question> byId = questions.findByIdIn(mock.getQuestionIds()).stream()
                    .collect(Collectors.toMap(Question::getId, Function.identity()));
            list = mock.getQuestionIds().stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        } else {
            list = questions.findBySectionInOrderByPositionAsc(mock.getSections());
        }
        Map<Section, List<Question>> grouped = list.stream()
                .collect(Collectors.groupingBy(Question::getSection, () -> new EnumMap<>(Section.class), Collectors.toList()));
        Map<Section, List<Question>> ordered = new LinkedHashMap<>();
        mock.getSections().forEach(s -> ordered.put(s, grouped.getOrDefault(s, List.of())));
        return ordered;
    }
}

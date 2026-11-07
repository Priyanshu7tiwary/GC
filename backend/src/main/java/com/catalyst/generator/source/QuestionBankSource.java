package com.catalyst.generator.source;

import com.catalyst.common.domain.Section;
import com.catalyst.generator.TargetDifficulty;
import com.catalyst.question.Question;
import com.catalyst.question.QuestionRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Picks from the stored bank, preferring the requested difficulty and the learner's weak topics. */
@Component
class QuestionBankSource implements QuestionSource {
    private final QuestionRepository questions;

    QuestionBankSource(QuestionRepository questions) {
        this.questions = questions;
    }

    @Override
    public List<Question> fetch(Section section, int count, TargetDifficulty difficulty, List<String> preferredTopics) {
        List<Question> pool = questions.findBySectionInOrderByPositionAsc(List.of(section));
        List<Question> leveled = difficulty.fixedLevel()
                .map(level -> pool.stream().filter(q -> q.getDifficulty() == level).toList())
                .filter(l -> !l.isEmpty())
                .orElse(pool);
        Set<String> preferred = Set.copyOf(preferredTopics);
        return leveled.stream()
                .sorted(Comparator.comparing((Question q) -> !preferred.contains(q.getTopic())).thenComparingInt(Question::getPosition))
                .limit(count)
                .toList();
    }
}

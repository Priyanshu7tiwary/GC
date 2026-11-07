package com.catalyst.attempt.scoring;

import com.catalyst.common.domain.Section;
import com.catalyst.question.Question;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Scores a set of responses using the configured answer checker and marking scheme. */
@Component
public class Scorer {
    private final AnswerChecker checker;
    private final MarkingScheme marking;

    public Scorer(AnswerChecker checker, MarkingScheme marking) {
        this.checker = checker;
        this.marking = marking;
    }

    public ScoreCard score(Map<Section, List<Question>> paper, Map<String, String> responses, Map<Section, Integer> sectionSeconds) {
        Map<Section, SectionResult> sections = new LinkedHashMap<>();
        Map<String, Outcome> outcomes = new HashMap<>();
        int total = 0, max = 0;
        for (var entry : paper.entrySet()) {
            int correct = 0, wrong = 0, skipped = 0, score = 0, secMax = 0;
            for (Question q : entry.getValue()) {
                Outcome o = checker.check(q, responses.get(q.getId()));
                outcomes.put(q.getId(), o);
                score += marking.marks(q.getType(), o);
                secMax += marking.correct(q.getType());
                switch (o) {
                    case CORRECT -> correct++;
                    case WRONG -> wrong++;
                    case SKIPPED -> skipped++;
                }
            }
            sections.put(entry.getKey(), new SectionResult(correct, wrong, skipped, score, secMax,
                    sectionSeconds.getOrDefault(entry.getKey(), 0)));
            total += score;
            max += secMax;
        }
        return new ScoreCard(total, max, sections, outcomes);
    }
}

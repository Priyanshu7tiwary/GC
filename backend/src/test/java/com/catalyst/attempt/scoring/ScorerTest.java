package com.catalyst.attempt.scoring;

import com.catalyst.common.domain.Section;
import com.catalyst.question.Question;
import com.catalyst.question.QuestionType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScorerTest {
    private final AnswerChecker checker = new AnswerChecker(List.of(new McqAnswerEvaluator(), new TitaAnswerEvaluator()));
    private final Scorer scorer = new Scorer(checker, new CatMarkingScheme());

    @Test
    void appliesCatMarkingPerSection() {
        Map<Section, List<Question>> paper = new LinkedHashMap<>();
        paper.put(Section.QA, List.of(
                q("q1", Section.QA, QuestionType.MCQ, "1"),   // correct  +3
                q("q2", Section.QA, QuestionType.MCQ, "2"),   // wrong    -1
                q("q3", Section.QA, QuestionType.TITA, "48"), // wrong TITA 0
                q("q4", Section.QA, QuestionType.TITA, "10"))); // skipped 0
        ScoreCard card = scorer.score(paper, Map.of("q1", "1", "q2", "0", "q3", "47"), Map.of(Section.QA, 600));

        assertThat(card.score()).isEqualTo(2);
        assertThat(card.maxScore()).isEqualTo(12);
        SectionResult qa = card.sections().get(Section.QA);
        assertThat(qa.correct()).isEqualTo(1);
        assertThat(qa.wrong()).isEqualTo(2);
        assertThat(qa.unattempted()).isEqualTo(1);
        assertThat(qa.timeSeconds()).isEqualTo(600);
        assertThat(card.outcomes()).containsEntry("q4", Outcome.SKIPPED);
    }

    @Test
    void questionsWithoutAVerifiedKeyAreNeitherScoredNorCounted() {
        Map<Section, List<Question>> paper = new LinkedHashMap<>();
        paper.put(Section.QA, List.of(
                q("k1", Section.QA, QuestionType.MCQ, "1"),     // correct +3
                q("u1", Section.QA, QuestionType.MCQ, null),    // no key: answered, still 0
                q("u2", Section.QA, QuestionType.TITA, null))); // no key: skipped
        ScoreCard card = scorer.score(paper, Map.of("k1", "1", "u1", "0"), Map.of());

        assertThat(card.score()).isEqualTo(3);
        assertThat(card.maxScore()).isEqualTo(3);
        SectionResult qa = card.sections().get(Section.QA);
        assertThat(qa.correct() + qa.wrong() + qa.unattempted()).isEqualTo(1);
        assertThat(card.outcomes()).containsEntry("u1", Outcome.UNSCORED).containsEntry("u2", Outcome.UNSCORED);
    }

    @Test
    void titaComparesNumbersByValueAndSequencesAsText() {
        assertThat(checker.check(q("a", Section.QA, QuestionType.TITA, "48"), "48.0")).isEqualTo(Outcome.CORRECT);
        assertThat(checker.check(q("b", Section.VARC, QuestionType.TITA, "2413"), " 2413 ")).isEqualTo(Outcome.CORRECT);
        assertThat(checker.check(q("c", Section.VARC, QuestionType.TITA, "2413"), "2431")).isEqualTo(Outcome.WRONG);
    }

    @Test
    void mcqRejectsNonNumericResponses() {
        assertThat(checker.check(q("m", Section.QA, QuestionType.MCQ, "1"), "B")).isEqualTo(Outcome.WRONG);
    }

    static Question q(String id, Section s, QuestionType type, String answer) {
        try {
            var ctor = Question.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Question q = ctor.newInstance();
            set(q, "id", id);
            set(q, "section", s);
            set(q, "type", type);
            set(q, "answer", answer);
            set(q, "options", List.of("a", "b", "c", "d"));
            set(q, "topic", "t");
            return q;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void set(Object o, String field, Object v) throws ReflectiveOperationException {
        Field f = o.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(o, v);
    }
}

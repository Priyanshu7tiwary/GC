package com.catalyst.generator.plan;

import com.catalyst.common.domain.Section;
import com.catalyst.question.Difficulty;
import com.catalyst.question.Question;
import com.catalyst.question.QuestionOrigin;
import com.catalyst.question.QuestionType;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class CoveragePickerTest {
    private static int pos = 0;

    public static Question q(String id, Section s, String topicId, QuestionType type, String passage) {
        Question q = Question.create(id, QuestionOrigin.BANK, s, null, topicId, type, "Q " + id, passage,
                type == QuestionType.MCQ ? List.of("a", "b", "c", "d") : List.of(), "1", "e", Difficulty.MODERATE, ++pos, Instant.EPOCH);
        set(q, "topicId", topicId);
        return q;
    }

    static void set(Object o, String field, Object v) {
        try {
            Field f = Question.class.getDeclaredField(field);
            f.setAccessible(true);
            f.set(o, v);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static final Map<String, String> CHAPTER = new HashMap<>(Map.of("t1", "C1", "t2", "C1", "t3", "C2", "t4", "C2", "empty", "C2"));

    static List<Question> qaPool() {
        List<Question> pool = new ArrayList<>();
        for (String t : List.of("t1", "t2", "t3", "t4")) {
            for (int i = 0; i < 10; i++) pool.add(q(t + "-" + i, Section.QA, t, i % 3 == 0 ? QuestionType.TITA : QuestionType.MCQ, null));
        }
        return pool;
    }

    @RepeatedTest(10)
    void fillsEverySlotWithItsTopicAndKeepsTitaNearThirty() {
        SectionPlan plan = new SectionPlan(Section.QA, 12, 0, List.of(), List.of(
                new SectionPlan.Slot("t1", "t1", "C1", 3), new SectionPlan.Slot("t2", "t2", "C1", 3),
                new SectionPlan.Slot("t3", "t3", "C2", 3), new SectionPlan.Slot("t4", "t4", "C2", 3)));
        var sel = new CoveragePicker(new Random()).pick(plan, qaPool(), Optional.empty(), Set.of(), CHAPTER);
        assertThat(sel.questions()).hasSize(12).doesNotHaveDuplicates();
        Map<String, Long> perTopic = sel.questions().stream().collect(Collectors.groupingBy(Question::getTopicId, Collectors.counting()));
        assertThat(perTopic).containsEntry("t1", 3L).containsEntry("t2", 3L).containsEntry("t3", 3L).containsEntry("t4", 3L);
        long tita = sel.questions().stream().filter(x -> x.getType() == QuestionType.TITA).count();
        assertThat(tita).isBetween(3L, 5L);
        assertThat(sel.shortfall()).isZero();
    }

    @Test
    void fallsBackToTheSameChapterWhenATopicIsEmpty() {
        SectionPlan plan = new SectionPlan(Section.QA, 2, 0, List.of(), List.of(new SectionPlan.Slot("empty", "Empty topic", "C2", 2)));
        var sel = new CoveragePicker(new Random(1)).pick(plan, qaPool(), Optional.empty(), Set.of(), CHAPTER);
        assertThat(sel.questions()).hasSize(2).allMatch(x -> CHAPTER.get(x.getTopicId()).equals("C2"));
        assertThat(sel.shortfall()).isEqualTo(2);
        assertThat(sel.notes()).anyMatch(n -> n.contains("Empty topic"));
    }

    @Test
    void avoidsQuestionsAlreadySeenWhileOthersRemain() {
        SectionPlan plan = new SectionPlan(Section.QA, 5, 0, List.of(), List.of(new SectionPlan.Slot("t1", "t1", "C1", 5)));
        Set<String> seen = qaPool().stream().filter(x -> x.getTopicId().equals("t1")).limit(5).map(Question::getId).collect(Collectors.toSet());
        for (int run = 0; run < 10; run++) {
            var sel = new CoveragePicker(new Random(run)).pick(plan, qaPool(), Optional.empty(), seen, CHAPTER);
            assertThat(sel.questions()).extracting(Question::getId).doesNotContainAnyElementsOf(seen);
        }
    }

    @RepeatedTest(30)
    void neverLeavesAStubOfOneOrTwoQuestionsWhenSetsCanAddUpExactly() {
        List<Question> pool = new ArrayList<>();
        int n = 0;
        for (int size : new int[] {3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6}) {
            String t = "t" + (1 + n % 4);
            for (int i = 0; i < size; i++) pool.add(q("u" + n + "-" + i, Section.DILR, t, QuestionType.MCQ, "set " + n));
            n++;
        }
        SectionPlan plan = new SectionPlan(Section.DILR, 22, 22, List.of(
                new SectionPlan.Slot("t1", "t1", "C1", 1), new SectionPlan.Slot("t2", "t2", "C1", 1),
                new SectionPlan.Slot("t3", "t3", "C2", 1), new SectionPlan.Slot("t4", "t4", "C2", 1)), List.of());
        var sel = new CoveragePicker(new Random()).pick(plan, pool, Optional.empty(), Set.of(), CHAPTER);
        assertThat(sel.questions()).hasSize(22);
        Map<String, Long> perSet = sel.questions().stream().collect(Collectors.groupingBy(Question::getPassage, Collectors.counting()));
        assertThat(perSet.values()).allMatch(c -> c >= 3);
    }

    @RepeatedTest(10)
    void takesWholeSetsOnDistinctPlannedTopicsAndOnlyTrimsTheLast() {
        List<Question> pool = new ArrayList<>();
        for (String t : List.of("t1", "t2", "t3", "t4")) {
            for (int s = 0; s < 3; s++) {
                for (int i = 0; i < 5; i++) pool.add(q(t + "-s" + s + "-" + i, Section.DILR, t, QuestionType.MCQ, "set " + t + s));
            }
        }
        SectionPlan plan = new SectionPlan(Section.DILR, 22, 22, List.of(
                new SectionPlan.Slot("t1", "t1", "C1", 1), new SectionPlan.Slot("t3", "t3", "C2", 1),
                new SectionPlan.Slot("t2", "t2", "C1", 1), new SectionPlan.Slot("t4", "t4", "C2", 1)), List.of());
        var sel = new CoveragePicker(new Random()).pick(plan, pool, Optional.empty(), Set.of(), CHAPTER);
        assertThat(sel.questions()).hasSize(22).doesNotHaveDuplicates();
        Map<String, Long> perSet = sel.questions().stream().collect(Collectors.groupingBy(Question::getPassage, Collectors.counting()));
        assertThat(perSet.values().stream().filter(n -> n < 5).count()).isLessThanOrEqualTo(1);
        assertThat(sel.topicIds()).containsExactlyInAnyOrder("t1", "t2", "t3", "t4");
    }
}

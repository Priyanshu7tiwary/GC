package com.catalyst.generator.plan;

import com.catalyst.common.domain.Section;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BlueprintPlannerTest {
    static List<TopicStats> qaTopics() {
        List<TopicStats> t = new ArrayList<>();
        String[][] chapters = {{"Arithmetic", "Percentages", "Profit", "Interest", "Ratio", "Mixtures", "Work", "Speed", "Averages"},
                {"Algebra", "Linear", "Quadratic", "Inequalities", "Functions", "Logs", "Progressions", "Surds"},
                {"Number System", "Divisibility", "HCF", "Factors", "Base", "Digits"},
                {"Geometry", "Triangles", "Circles", "Polygons", "Coordinate", "3D", "Trig"},
                {"Modern Maths", "PnC", "Probability", "Sets", "Series"}};
        long w = 40;
        for (String[] ch : chapters) {
            for (int i = 1; i < ch.length; i++) t.add(new TopicStats("qa-" + ch[i], Section.QA, ch[0], ch[i], w--, ch[i].equals("Base") ? 0 : 20));
        }
        return t;
    }

    @Test
    void qaSlotsAddUpCoverEveryChapterAndPreferDistinctTopics() {
        SectionPlan p = BlueprintPlanner.plan(Section.QA, 22, qaTopics(), Set.of(), false);
        assertThat(p.slots().stream().mapToInt(SectionPlan.Slot::count).sum()).isEqualTo(22);
        assertThat(p.slots().stream().map(SectionPlan.Slot::chapter).collect(Collectors.toSet())).hasSize(5);
        assertThat(p.slots()).hasSize(22);  // 30 usable topics: no topic needs a second question
        assertThat(p.slots()).noneMatch(s -> s.topic().equals("Base"));  // no questions in the bank: never planned
    }

    @Test
    void weakTopicsAreGuaranteedASlotWhenFocusIsOn() {
        Set<String> weak = Set.of("Series", "Trig", "Digits");
        SectionPlan p = BlueprintPlanner.plan(Section.QA, 22, qaTopics(), weak, true);
        assertThat(p.slots().stream().map(SectionPlan.Slot::topic)).containsAll(weak);
        assertThat(p.slots().stream().mapToInt(SectionPlan.Slot::count).sum()).isEqualTo(22);
    }

    @Test
    void varcIsMostlyReadingComprehensionWithEveryVerbalAbilityType() {
        List<TopicStats> t = List.of(
                new TopicStats("rc-1", Section.VARC, "Reading Comprehension", "Inference questions", 300, 300),
                new TopicStats("va-1", Section.VARC, "Verbal Ability", "Para jumbles", 65, 70),
                new TopicStats("va-2", Section.VARC, "Verbal Ability", "Para summary", 70, 75),
                new TopicStats("va-3", Section.VARC, "Verbal Ability", "Odd sentence out", 48, 54),
                new TopicStats("va-4", Section.VARC, "Verbal Ability", "Sentence placement", 27, 33));
        SectionPlan p = BlueprintPlanner.plan(Section.VARC, 22, t, Set.of(), false);
        assertThat(p.passageQuestions()).isEqualTo(15);
        assertThat(p.slots().stream().mapToInt(SectionPlan.Slot::count).sum()).isEqualTo(7);
        assertThat(p.slots()).hasSize(4);
    }

    @Test
    void dilrSetsCoverDistinctTopicsAlternatingDiAndLr() {
        List<TopicStats> t = List.of(
                new TopicStats("di-1", Section.DILR, "Data Interpretation", "Tables", 116, 120),
                new TopicStats("di-2", Section.DILR, "Data Interpretation", "Graphs", 24, 25),
                new TopicStats("lr-1", Section.DILR, "Logical Reasoning", "Games", 72, 75),
                new TopicStats("lr-2", Section.DILR, "Logical Reasoning", "Scheduling", 65, 70));
        SectionPlan p = BlueprintPlanner.plan(Section.DILR, 22, t, Set.of(), false);
        assertThat(p.passageQuestions()).isEqualTo(22);
        assertThat(p.setTopics()).extracting(SectionPlan.Slot::topic).containsExactly("Tables", "Games", "Graphs", "Scheduling");
    }

    @Test
    void largestRemainderAlwaysHandsOutExactlyN() {
        var m = BlueprintPlanner.largestRemainder(new java.util.LinkedHashMap<>(java.util.Map.of("a", 1.0, "b", 1.0, "c", 1.0)), 22);
        assertThat(m.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(22);
    }
}

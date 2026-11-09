package com.catalyst.generator.steps;

import com.catalyst.common.domain.Section;
import com.catalyst.generator.GenerationContext;
import com.catalyst.generator.GenerationStep;
import com.catalyst.generator.plan.BlueprintPlanner;
import com.catalyst.generator.plan.SectionPlan;
import com.catalyst.generator.plan.TopicStats;
import com.catalyst.question.QuestionOrigin;
import com.catalyst.question.QuestionRepository;
import com.catalyst.syllabus.TopicRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Plans each section from the database: chapter weights follow how often each chapter appears in the official CAT
 * papers, slots are spread over distinct topics, and only topics with questions in the bank are planned.
 */
@Component
@Order(20)
class BlueprintStep implements GenerationStep {
    private final int questionsPerSection;
    private final TopicRepository topics;
    private final QuestionRepository questions;

    BlueprintStep(@Value("${app.generator.questions-per-section:22}") int questionsPerSection, TopicRepository topics, QuestionRepository questions) {
        this.questionsPerSection = questionsPerSection;
        this.topics = topics;
        this.questions = questions;
    }

    public String id() { return "blueprint"; }
    public String label() { return "Building the test blueprint"; }
    public String detail() { return "Spreading questions across chapters and topics the way recent CAT papers do"; }

    @Override
    public String execute(GenerationContext ctx) {
        Map<String, long[]> counts = new HashMap<>();  // topicId -> {pyq questions, keyed questions}
        for (Object[] row : questions.countByTopicAndOrigin()) {
            if (row[1] == null) continue;
            long[] c = counts.computeIfAbsent((String) row[1], k -> new long[2]);
            if (row[2] == QuestionOrigin.PYQ) c[0] += (Long) row[3];
            c[1] += (Long) row[4];
        }
        List<TopicStats> stats = topics.findAllByOrderByPositionAsc().stream()
                .map(t -> new TopicStats(t.getId(), t.getSection(), t.getChapter(), t.getName(),
                        counts.getOrDefault(t.getId(), new long[2])[0], counts.getOrDefault(t.getId(), new long[2])[1]))
                .toList();
        var weak = new HashSet<>(ctx.weakTopics());
        for (Section s : ctx.sections()) {
            ctx.blueprint().put(s, questionsPerSection);
            ctx.plans().put(s, BlueprintPlanner.plan(s, questionsPerSection, stats, weak, ctx.request().focusWeakTopics()));
        }
        return ctx.plans().values().stream().map(BlueprintStep::describe).collect(Collectors.joining(" · "))
                + " · " + ctx.request().difficulty().label();
    }

    static String describe(SectionPlan p) {
        return switch (p.section()) {
            case QA -> {
                Map<String, Integer> byChapter = new LinkedHashMap<>();
                p.slots().forEach(s -> byChapter.merge(s.chapter(), s.count(), Integer::sum));
                yield "QA " + p.total() + " (" + p.slots().size() + " topics: " + byChapter.entrySet().stream()
                        .map(e -> e.getKey() + " " + e.getValue()).collect(Collectors.joining(", ")) + ")";
            }
            case VARC -> "VARC " + p.total() + " (" + p.passageQuestions() + " RC, " + (p.total() - p.passageQuestions()) + " VA over "
                    + p.slots().size() + " types)";
            case DILR -> "DILR " + p.total() + " (sets led by " + p.setTopics().stream().limit(3).map(SectionPlan.Slot::topic)
                    .collect(Collectors.joining(", ")) + ")";
        };
    }
}

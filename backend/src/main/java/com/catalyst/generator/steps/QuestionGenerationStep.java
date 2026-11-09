package com.catalyst.generator.steps;

import com.catalyst.common.domain.Section;
import com.catalyst.generator.GenerationContext;
import com.catalyst.generator.GenerationStep;
import com.catalyst.generator.ai.DraftQuestion;
import com.catalyst.generator.ai.QuestionWriter;
import com.catalyst.generator.plan.CoveragePicker;
import com.catalyst.generator.plan.SectionPlan;
import com.catalyst.mock.Mock;
import com.catalyst.mock.MockKind;
import com.catalyst.mock.MockRepository;
import com.catalyst.question.Question;
import com.catalyst.question.QuestionRepository;
import com.catalyst.syllabus.Topic;
import com.catalyst.syllabus.TopicRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fills each section's blueprint from the question bank (see {@link CoveragePicker}). When Claude is configured,
 * it also drafts new standalone questions — first for topics the bank could not cover, then a few extra for
 * variety. Drafts are only candidates here; {@link AnswerValidationStep} keeps those an independent solve agrees with.
 */
@Component
@Order(30)
class QuestionGenerationStep implements GenerationStep {
    private static final Set<Section> AI_SECTIONS = Set.of(Section.QA, Section.VARC);

    private final QuestionRepository questions;
    private final TopicRepository topics;
    private final MockRepository mocks;
    private final ObjectProvider<QuestionWriter> writer;
    private final int aiPerSection;

    QuestionGenerationStep(QuestionRepository questions, TopicRepository topics, MockRepository mocks, ObjectProvider<QuestionWriter> writer,
                           @Value("${app.generator.ai-questions-per-section:3}") int aiPerSection) {
        this.questions = questions;
        this.topics = topics;
        this.mocks = mocks;
        this.writer = writer;
        this.aiPerSection = aiPerSection;
    }

    public String id() { return "generate"; }
    public String label() { return "Generating questions"; }
    public String detail() { return "Picking sets and questions for every planned topic; drafting new ones with AI"; }

    @Override
    public String execute(GenerationContext ctx) {
        Set<String> seen = new HashSet<>();
        for (Mock m : mocks.findByKindAndOwnerIdOrderByCreatedAtDesc(MockKind.CUSTOM, ctx.userId())) {
            if (m.getQuestionIds() != null) seen.addAll(m.getQuestionIds());
        }
        ctx.recentlyUsed(seen);
        Map<String, String> chapterOf = topics.findAllByOrderByPositionAsc().stream().collect(Collectors.toMap(Topic::getId, Topic::getChapter));
        CoveragePicker picker = new CoveragePicker(new Random());

        int topicsCovered = 0;
        Set<String> chapters = new LinkedHashSet<>();
        int fallbacks = 0;
        for (var e : ctx.plans().entrySet()) {
            List<Question> pool = questions.findBySectionAndAnswerIsNotNull(e.getKey());
            if (pool.isEmpty()) throw new IllegalStateException("There are no " + e.getKey() + " questions in the bank yet.");
            CoveragePicker.Selection sel = picker.pick(e.getValue(), pool, ctx.request().difficulty().fixedLevel(), seen, chapterOf);
            ctx.coverage().put(e.getKey(), sel);
            ctx.questions().put(e.getKey(), new ArrayList<>(sel.questions()));
            topicsCovered += sel.topicIds().size();
            chapters.addAll(sel.chapters());
            fallbacks += sel.shortfall();
        }
        int got = ctx.questions().values().stream().mapToInt(List::size).sum();
        int wanted = ctx.blueprint().values().stream().mapToInt(Integer::intValue).sum();
        String picked = got + " of " + wanted + " questions picked, covering " + topicsCovered + " topics in " + chapters.size() + " chapters"
                + (fallbacks > 0 ? " (" + fallbacks + " from a neighbouring topic)" : "");

        QuestionWriter w = writer.getIfAvailable();
        if (w == null || aiPerSection <= 0) return picked;
        int drafted = 0;
        for (var e : ctx.plans().entrySet()) {
            Section section = e.getKey();
            if (!AI_SECTIONS.contains(section)) continue;
            List<String> wanted_ = draftTopics(e.getValue(), ctx.coverage().get(section), ctx.weakTopics());
            if (wanted_.isEmpty()) continue;
            List<String> examples = ctx.questions().get(section).stream().filter(q -> q.getPassage() == null).limit(2)
                    .map(q -> q.getPrompt() + (q.getOptions() == null ? "" : "\nOptions: " + String.join(" | ", q.getOptions())))
                    .collect(Collectors.toList());
            List<DraftQuestion> drafts = w.draft(section, aiPerSection, wanted_, examples);
            ctx.drafts().put(section, drafts);
            drafted += drafts.size();
        }
        return picked + " · " + drafted + " new question" + (drafted == 1 ? "" : "s") + " drafted by AI";
    }

    /** Topics for AI drafts: planned topics the bank could not cover come first, then weak topics, then the rest. */
    static List<String> draftTopics(SectionPlan plan, CoveragePicker.Selection sel, List<String> weak) {
        Set<String> out = new LinkedHashSet<>();
        plan.slots().stream().filter(s -> !sel.topicIds().contains(s.topicId())).forEach(s -> out.add(s.topic()));
        plan.slots().stream().filter(s -> weak.contains(s.topic())).forEach(s -> out.add(s.topic()));
        plan.slots().forEach(s -> out.add(s.topic()));
        return new ArrayList<>(out).subList(0, Math.min(6, out.size()));
    }
}

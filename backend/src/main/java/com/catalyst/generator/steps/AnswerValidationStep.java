package com.catalyst.generator.steps;

import com.catalyst.common.domain.Section;
import com.catalyst.generator.GenerationContext;
import com.catalyst.generator.GenerationStep;
import com.catalyst.generator.ai.DraftQuestion;
import com.catalyst.generator.ai.QuestionWriter;
import com.catalyst.question.Difficulty;
import com.catalyst.question.Question;
import com.catalyst.question.QuestionOrigin;
import com.catalyst.question.QuestionRepository;
import com.catalyst.question.QuestionType;
import com.catalyst.syllabus.TopicRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Checks every answer key before a mock is assembled.
 *
 * Pool questions must have a well-formed key. AI drafts must pass structural checks and then an independent solve:
 * a second model call answers each draft without seeing its key, and only drafts where both agree are kept. Kept
 * drafts are saved to the question bank (origin AI) and take the place of standalone pool questions, so the section
 * keeps its size.
 */
@Component
@Order(40)
class AnswerValidationStep implements GenerationStep {
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    private final ObjectProvider<QuestionWriter> writer;
    private final QuestionRepository questions;
    private final Clock clock;
    private final TopicRepository topics;

    AnswerValidationStep(ObjectProvider<QuestionWriter> writer, QuestionRepository questions, Clock clock, TopicRepository topics) {
        this.writer = writer;
        this.topics = topics;
        this.questions = questions;
        this.clock = clock;
    }

    public String id() { return "validate"; }
    public String label() { return "Validating answers"; }
    public String detail() { return "Solving each question independently to verify the key"; }

    @Override
    public String execute(GenerationContext ctx) {
        int total = 0, valid = 0;
        for (var entry : ctx.questions().entrySet()) {
            List<Question> ok = entry.getValue().stream().filter(AnswerValidationStep::hasValidKey).toList();
            total += entry.getValue().size();
            valid += ok.size();
            if (ok.isEmpty()) throw new IllegalStateException("No usable questions for " + entry.getKey() + ".");
            entry.setValue(new ArrayList<>(ok));
        }
        // no question may appear twice, within or across sections
        Set<String> ids = new HashSet<>();
        for (var entry : ctx.questions().entrySet()) {
            for (Question q : entry.getValue()) {
                if (!ids.add(q.getId())) throw new IllegalStateException("Question " + q.getId() + " was picked twice.");
            }
        }
        String result = valid + "/" + total + " answer keys verified";

        QuestionWriter w = writer.getIfAvailable();
        int drafted = ctx.drafts().values().stream().mapToInt(List::size).sum();
        if (w == null || drafted == 0) return result;
        int kept = 0;
        for (var entry : ctx.drafts().entrySet()) {
            kept += keepVerified(entry.getKey(), entry.getValue(), w, ctx);
        }
        return result + " · " + kept + " of " + drafted + " AI questions passed an independent solve";
    }

    private int keepVerified(Section section, List<DraftQuestion> drafts, QuestionWriter w, GenerationContext ctx) {
        List<DraftQuestion> wellFormed = drafts.stream().filter(d -> wellFormed(d) && !questions.existsByPromptAndSection(d.prompt().trim(), section)).toList();
        if (wellFormed.isEmpty()) return 0;
        List<Optional<String>> solved = w.solve(wellFormed);
        List<Question> chosen = ctx.questions().get(section);
        int position = questions.maxPosition();
        int kept = 0;
        for (int i = 0; i < wellFormed.size(); i++) {
            DraftQuestion d = wellFormed.get(i);
            if (solved.get(i).isEmpty() || !sameAnswer(d, solved.get(i).get())) continue;
            int slot = replaceable(chosen);
            if (slot < 0) break;  // nothing standalone left to swap out
            String topicName = clip(d.topic(), 120);
            Question q = questions.save(Question.create("ai-" + UUID.randomUUID(), QuestionOrigin.AI, section,
                    topics.findBySectionAndNameIgnoreCase(section, topicName).orElse(null), topicName, "MCQ".equals(d.type()) ? QuestionType.MCQ : QuestionType.TITA, d.prompt().trim(), null,
                    "MCQ".equals(d.type()) ? d.options().stream().map(String::trim).toList() : List.of(), d.answer().trim(),
                    "AI-drafted question; the key was confirmed by an independent solve. " + (d.explanation() == null ? "" : d.explanation().trim()),
                    difficulty(d.difficulty()), ++position, clock.instant()));
            chosen.set(slot, q);
            kept++;
        }
        return kept;
    }

    /**
     * Which standalone bank question a verified AI question replaces: one from the topic with the most questions in
     * the section, so swapping in a new question never removes a topic from the test. Sets are never broken up.
     */
    static int replaceable(List<Question> chosen) {
        java.util.Map<String, Integer> perTopic = new java.util.HashMap<>();
        chosen.forEach(q -> perTopic.merge(String.valueOf(q.getTopicId()), 1, Integer::sum));
        int best = -1, bestCount = 0;
        for (int i = chosen.size() - 1; i >= 0; i--) {
            Question q = chosen.get(i);
            if (q.getPassage() != null || q.getOrigin() == QuestionOrigin.AI) continue;
            int c = perTopic.get(String.valueOf(q.getTopicId()));
            if (c > bestCount) { best = i; bestCount = c; }
        }
        return best;
    }

    static boolean wellFormed(DraftQuestion d) {
        if (d.prompt() == null || d.prompt().isBlank() || d.prompt().length() > 4000 || d.answer() == null) return false;
        if ("MCQ".equals(d.type())) {
            if (d.options() == null || d.options().size() != 4) return false;
            Set<String> distinct = new HashSet<>();
            for (String o : d.options()) {
                if (o == null || o.isBlank() || o.length() > 500 || !distinct.add(o.trim().toLowerCase(Locale.ROOT))) return false;
            }
            try {
                int k = Integer.parseInt(d.answer().trim());
                return k >= 0 && k < 4;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return "TITA".equals(d.type()) && NUMBER.matcher(d.answer().trim()).matches();
    }

    static boolean sameAnswer(DraftQuestion d, String solved) {
        String a = d.answer().trim(), b = solved.trim();
        if (a.equals(b)) return true;
        try {
            return new BigDecimal(a).compareTo(new BigDecimal(b)) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static Difficulty difficulty(String s) {
        try {
            return s == null ? Difficulty.MODERATE : Difficulty.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Difficulty.MODERATE;
        }
    }

    private static String clip(String s, int max) {
        String v = s == null || s.isBlank() ? "Mixed" : s.trim();
        return v.length() <= max ? v : v.substring(0, max);
    }

    static boolean hasValidKey(Question q) {
        if (q.getAnswer() == null || q.getAnswer().isBlank()) return false;
        if (q.getType() != QuestionType.MCQ) return true;
        try {
            int idx = Integer.parseInt(q.getAnswer().trim());
            return q.getOptions() != null && idx >= 0 && idx < q.getOptions().size();
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

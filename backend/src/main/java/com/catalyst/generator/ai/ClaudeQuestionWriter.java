package com.catalyst.generator.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonSchemaLocalValidation;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredOutputConfig;
import com.catalyst.common.domain.Section;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Claude-backed {@link QuestionWriter}. Active only when an Anthropic API key is configured (ANTHROPIC_API_KEY).
 *
 * Drafting and solving are separate requests: the solver sees each question without its key, so a draft is kept only
 * when two independent passes reach the same answer. Declined requests (stop reason "refusal") and API errors yield
 * no questions; mock generation then simply uses the existing pool.
 */
@Component
@ConditionalOnExpression("!'${app.ai.api-key:}'.isBlank()")
class ClaudeQuestionWriter implements QuestionWriter {
    private static final Logger log = LoggerFactory.getLogger(ClaudeQuestionWriter.class);

    private static final String DRAFT_SYSTEM = """
            You write original practice questions for the CAT (Common Admission Test for the IIMs), matching the \
            style, difficulty and conventions of recent CAT papers.

            Rules:
            - Every question must be original and self-contained. Don't reproduce questions from past papers or \
            coaching material; the examples you are shown are for style only.
            - MCQ questions have exactly four distinct options and one correct answer; give the answer as the \
            zero-based index of the correct option. TITA (type-in-the-answer) questions have a single numeric answer \
            (an integer unless the question says otherwise) and no options.
            - Write mathematical notation as TeX between \\( and \\), for example \\(\\frac{3}{4}\\) or \\(x^{2}\\).
            - Check each answer carefully before you finalise it: an independent solver will verify every key, and \
            questions whose key it can't reproduce are discarded.
            """;

    private static final String SOLVE_SYSTEM = """
            You are checking practice questions for the CAT. Solve each question on its own, carefully, and report \
            only your final answer: for an MCQ, the zero-based index of the correct option; for a TITA question, the \
            numeric answer, digits only. If a question is ambiguous or has no correct option, answer "invalid".
            """;

    private final AnthropicClient client;
    private final String model;

    ClaudeQuestionWriter(@Value("${app.ai.api-key}") String apiKey,
                         @Value("${app.ai.model:claude-opus-5-5}") String model) {
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).timeout(Duration.ofMinutes(5)).build();
        this.model = model;
    }

    @Override
    public List<DraftQuestion> draft(Section section, int count, List<String> topics, List<String> styleExamples) {
        String kind = switch (section) {
            case QA -> "Quantitative Aptitude questions (a mix of MCQ and TITA, as in recent papers)";
            case VARC -> "Verbal Ability questions only (para summary and odd-sentence-out as MCQ, para jumbles as TITA "
                    + "with a 4-digit sentence order); no reading-comprehension passages";
            case DILR -> "standalone Logical Reasoning questions";
        };
        String request = "Write " + count + " new " + kind + ".\n\n"
                + "Prefer these topics: " + String.join(", ", topics) + ".\n"
                + "Set each question's topic to exactly one of these syllabus topic names: " + String.join(", ", syllabusTopics(section)) + ".\n\n"
                + "Style examples from past CAT papers (do not copy them):\n\n"
                + styleExamples.stream().map(e -> "---\n" + e).collect(Collectors.joining("\n"));
        StructuredMessageCreateParams<DraftQuestion.Batch> params = base(DraftQuestion.Batch.class, DRAFT_SYSTEM, OutputConfig.Effort.HIGH)
                .addUserMessage(request)
                .build();
        return call(params).map(DraftQuestion.Batch::questions).orElse(List.of());
    }

    private static List<String> syllabusTopics(Section section) {
        return switch (section) {
            case QA -> List.of("Percentages", "Profit, Loss & Discount", "Simple & Compound Interest", "Ratio & Proportion",
                    "Mixtures & Alligation", "Time & Work", "Time, Speed & Distance", "Averages", "Linear equations",
                    "Quadratic equations", "Inequalities & Modulus", "Functions & Graphs", "Logarithms", "Progressions (AP/GP/HP)",
                    "Surds & Indices", "Divisibility & Remainders", "HCF & LCM", "Factors", "Base systems", "Unit & last digits",
                    "Triangles", "Circles", "Polygons & Quadrilaterals", "Coordinate geometry", "3D Mensuration",
                    "Trigonometry basics", "Permutations & Combinations", "Probability", "Set theory", "Sequences & Series");
            case VARC -> List.of("Para jumbles", "Para summary", "Odd sentence out", "Sentence placement");
            case DILR -> List.of("Arrangements (linear/circular)", "Scheduling & Calendars", "Selections & Distributions", "Quant-based LR");
        };
    }

    record Answer(@JsonPropertyDescription("The question number as given") int number,
                  @JsonPropertyDescription("Zero-based option index for MCQ, the number for TITA, or \"invalid\"") String answer) {}

    record Answers(List<Answer> answers) {}

    @Override
    public List<Optional<String>> solve(List<DraftQuestion> drafts) {
        StringBuilder sb = new StringBuilder("Solve these questions.\n");
        for (int i = 0; i < drafts.size(); i++) {
            DraftQuestion d = drafts.get(i);
            sb.append("\nQuestion ").append(i + 1).append(" (").append(d.type()).append(")\n").append(d.prompt()).append('\n');
            if ("MCQ".equals(d.type())) {
                for (int k = 0; k < d.options().size(); k++) sb.append("  [").append(k).append("] ").append(d.options().get(k)).append('\n');
            }
        }
        StructuredMessageCreateParams<Answers> params = base(Answers.class, SOLVE_SYSTEM, OutputConfig.Effort.HIGH)
                .addUserMessage(sb.toString())
                .build();
        List<Optional<String>> out = new ArrayList<>();
        drafts.forEach(d -> out.add(Optional.empty()));
        call(params).ifPresent(a -> a.answers().forEach(ans -> {
            if (ans.number() >= 1 && ans.number() <= drafts.size() && ans.answer() != null && !"invalid".equalsIgnoreCase(ans.answer().trim())) {
                out.set(ans.number() - 1, Optional.of(ans.answer().trim()));
            }
        }));
        return out;
    }

    private <T> StructuredMessageCreateParams.Builder<T> base(Class<T> schema, String system, OutputConfig.Effort effort) {
        return MessageCreateParams.builder()
                .model(model)
                .maxTokens(16000L)
                .system(system)
                .outputConfig(StructuredOutputConfig.<T>builder()
                        .format(schema, JsonSchemaLocalValidation.YES)
                        .effort(effort)
                        .build())
                // if a safety classifier declines, let the API retry on its recommended fallback model
                .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
    }

    private <T> Optional<T> call(StructuredMessageCreateParams<T> params) {
        try {
            StructuredMessage<T> response = client.messages().create(params);
            if (response.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
                log.info("Claude declined a question-writing request");
                return Optional.empty();
            }
            if (response.stopReason().filter(StopReason.MAX_TOKENS::equals).isPresent()) {
                log.warn("Claude's question-writing response hit max_tokens; discarding it");
                return Optional.empty();
            }
            return response.content().stream().flatMap(b -> b.text().stream()).map(t -> t.text()).findFirst();
        } catch (AnthropicException | IllegalStateException e) {
            log.warn("Claude question-writing call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}

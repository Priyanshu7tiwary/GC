package com.catalyst.suggestion.ai;

import com.catalyst.attempt.scoring.PercentileEstimator;
import com.catalyst.common.error.ServiceUnavailableException;
import com.catalyst.common.error.TooManyRequestsException;
import com.catalyst.mock.MockCatalogService;
import com.catalyst.mock.MockKind;
import com.catalyst.mock.dto.MockSummary;
import com.catalyst.practice.PracticeService;
import com.catalyst.syllabus.Topic;
import com.catalyst.syllabus.TopicRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Sends the learner's context to an OpenAI-compatible chat API (Groq, OpenRouter, a local Ollama, ...) and turns the
 * answer into a study plan: topics to cover and tests to attempt.
 *
 * The model can only choose from the topics and tests it is given. Its answer is checked against them, unknown ids
 * are dropped, and the links are built here, so a made-up topic or URL can never reach the learner.
 */
@Service
public class AiSuggestionService {
    private static final Logger log = LoggerFactory.getLogger(AiSuggestionService.class);
    private static final int MAX_TOPICS = 6, MAX_TESTS = 4;
    private static final Duration WINDOW = Duration.ofDays(7);

    private final TopicRepository topics;
    private final MockCatalogService mocks;
    private final PracticeService practice;
    private final ObjectMapper json;
    private final Clock clock;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Map<UUID, Object> locks = new ConcurrentHashMap<>();
    private final AiSuggestionRunRepository runs;
    private final PercentileEstimator percentiles;
    private final int weeklyLimit;
    private final String apiKey, baseUrl, model, fallbackModel;

    public AiSuggestionService(TopicRepository topics, MockCatalogService mocks, PracticeService practice, AiSuggestionRunRepository runs, PercentileEstimator percentiles, ObjectMapper json, Clock clock,
                               @Value("${app.suggestion-ai.weekly-limit:3}") int weeklyLimit,
                               @Value("${app.suggestion-ai.api-key:}") String apiKey,
                               @Value("${app.suggestion-ai.base-url:https://api.groq.com/openai/v1}") String baseUrl,
                               @Value("${app.suggestion-ai.model:openai/gpt-oss-120b}") String model,
                               @Value("${app.suggestion-ai.fallback-model:openai/gpt-oss-20b}") String fallbackModel) {
        this.topics = topics;
        this.mocks = mocks;
        this.practice = practice;
        this.runs = runs;
        this.percentiles = percentiles;
        this.weeklyLimit = weeklyLimit;
        this.json = json;
        this.clock = clock;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.model = model;
        this.fallbackModel = fallbackModel == null ? "" : fallbackModel.trim();
    }

    public boolean available() {
        return !apiKey.isBlank();
    }

    /** The latest plan and the weekly allowance: what the panel shows when it opens. Costs no allowance. */
    @Transactional(readOnly = true)
    public AiPlanState state(UUID userId) {
        AiSuggestionResponse plan = runs.findFirstByUserIdOrderByCreatedAtDesc(userId).map(this::read).orElse(null);
        return new AiPlanState(available(), plan, quota(userId));
    }

    /**
     * Generates a new plan, if this learner still has one left in the rolling 7 days. The allowance is only used
     * when a plan was actually produced, so a failed call costs nothing.
     */
    public AiPlanState suggest(UUID userId, AiSuggestionRequest req) {
        if (!available()) throw new ServiceUnavailableException("AI suggestions aren't set up on this server (no AI_API_KEY).");
        synchronized (locks.computeIfAbsent(userId, k -> new Object())) {  // two quick clicks can't both pass the check
            AiPlanState.Quota before = quota(userId);
            if (before.remaining() <= 0) {
                throw new TooManyRequestsException("You've used all " + weeklyLimit + " AI plans for this week. The next one opens in "
                        + daysUntil(before.nextAvailableAt()) + ".");
            }
            Instant now = clock.instant();
            AiSuggestionResponse out = generate(userId, req, now);
            try {
                runs.save(new AiSuggestionRun(userId, now, json.writeValueAsString(out)));
            } catch (Exception e) {
                log.error("AI suggestions: could not save the plan: {}", e.toString());
                throw new ServiceUnavailableException("Couldn't save your AI plan. Try again.");
            }
            return new AiPlanState(true, out, quota(userId));
        }
    }

    private AiPlanState.Quota quota(UUID userId) {
        Instant now = clock.instant();
        List<AiSuggestionRun> recent = runs.findByUserIdAndCountedTrueAndCreatedAtAfterOrderByCreatedAtAsc(userId, now.minus(WINDOW));
        int used = Math.min(recent.size(), weeklyLimit);
        int remaining = Math.max(0, weeklyLimit - recent.size());
        // the allowance comes back when the oldest of the counted plans is 7 days old
        Instant next = remaining > 0 ? null : recent.get(recent.size() - weeklyLimit).getCreatedAt().plus(WINDOW);
        return new AiPlanState.Quota(weeklyLimit, used, remaining, next);
    }

    private String daysUntil(Instant at) {
        long hours = Math.max(1, Duration.between(clock.instant(), at).toHours());
        return hours >= 24 ? (hours + 23) / 24 + (((hours + 23) / 24) == 1 ? " day" : " days") : hours + (hours == 1 ? " hour" : " hours");
    }

    private AiSuggestionResponse read(AiSuggestionRun run) {
        try {
            return json.readValue(run.getResponse(), AiSuggestionResponse.class);
        } catch (Exception e) {
            log.warn("AI suggestions: stored plan could not be read: {}", e.toString());
            return null;
        }
    }

    private AiSuggestionResponse generate(UUID userId, AiSuggestionRequest req, Instant now) {
        Map<String, Topic> byId = topics.findAllByOrderByPositionAsc().stream().collect(Collectors.toMap(Topic::getId, t -> t, (a, b) -> a, LinkedHashMap::new));
        Map<String, PracticeService.TopicCounts> bank = practice.summary(userId);
        Set<String> attempted = new LinkedHashSet<>(req.attemptedMockIds() == null ? List.of() : req.attemptedMockIds());
        // only previous-year papers: the sectional mocks do not have curated question sets yet
        List<MockSummary> catalogue = new ArrayList<>(mocks.list(userId, MockKind.PREVIOUS_YEAR, null, null));
        Map<String, MockSummary> mockById = catalogue.stream().collect(Collectors.toMap(MockSummary::id, m -> m, (a, b) -> a, LinkedHashMap::new));

        Reply reply = chat(systemPrompt(), userPrompt(req, byId, bank, mockById, attempted));
        return parse(reply.content(), reply.model(), req, byId, bank, mockById, attempted, now);
    }

    /* ---- prompt ---- */

    private static String systemPrompt() {
        return """
                You are an experienced, candid CAT (Common Admission Test, India) coach. You get one learner's progress, the numbers
                behind their goal, and the full list of topics and tests they can use. Judge honestly where they stand, then plan their
                next study block.
                Rules:
                - Reply with ONE JSON object and nothing else, in this shape:
                  {"summary": string,
                   "likelihood": {"percent": integer 1-99, "confidence": "low"|"medium"|"high", "reason": string, "realisticPercentile": number},
                   "topics": [{"topicId": string, "priority": "high"|"medium"|"low", "reason": string}],
                   "tests": [{"mockId": string, "reason": string}]}
                - Be honest, not encouraging for its own sake. No flattery, no "you can do it", no softening. If the target is out of
                  reach in the days left, say so plainly in the summary and name a realistic percentile to aim for instead. If they are
                  on track, say that just as plainly, and say what could still go wrong.
                - "likelihood.percent" is your estimate of the chance they reach the target percentile in this CAT. It must agree with
                  the GOAL numbers: a recent average far below the score needed, with few days left, means a low percent (often under
                  15). Large gaps rarely close by more than a few marks a week. "likelihood.reason" gives the deciding numbers in one or
                  two sentences.
                - "likelihood.confidence" is "low" when there are no full-length mock results, "medium" with one or two, "high" with more.
                - When the current level is UNKNOWN, say so. Do not call their level poor or nil, and do not describe a sectional or blank test
                  as their result. Judge the likelihood from the days left and how demanding the target is (99+ means beating about 99%% of
                  roughly 3 lakh candidates), and recommend a full-length mock to find out.
                - Use the days left exactly as given, and weigh the TIME numbers: compare the hours available at a realistic daily pace
                  with the hours CAT preparation usually needs for this target. With many months left there is real time to improve;
                  with a few weeks there is not. Say how many hours a day the target would take if that matters.
                - "likelihood.realisticPercentile" is the percentile you think they can actually reach by exam day with steady work from now.
                  Always give it; when the target is unlikely, the summary must name it. Base it on full-length results and the time left.
                  With no full-length results, use what committed CAT aspirants typically reach in that much time, never a sectional or
                  blank test.
                - Choose %d to %d topics and %d to %d tests. Use ONLY topicId and mockId values from the lists you are given, copied exactly.
                - Favour topics that are not covered yet or in progress, that carry many marks (marked high-yield), that fit the learner's weak
                  sections, or that are overdue for revision. Spread them over sections unless one section is clearly the problem.
                - Favour tests the learner has not attempted; they may take only some sections of a paper. With no attempts yet, start with
                  one recent previous-year paper.
                - "reason" is one or two plain sentences that use the learner's own numbers. "summary" is two or three sentences on where they
                  stand and what to do first. No markdown, no links.
                """.formatted(3, MAX_TOPICS, 2, MAX_TESTS);
    }

    private String userPrompt(AiSuggestionRequest r, Map<String, Topic> byId, Map<String, PracticeService.TopicCounts> bank,
                              Map<String, MockSummary> mockById, Set<String> attempted) {
        StringBuilder b = new StringBuilder();
        b.append(goal(r)).append('\n');
        b.append("LEARNER\n");
        b.append("target: ").append(r.targetPercentile()).append(" percentile in CAT ").append(r.targetYear()).append(", ").append(r.daysLeft()).append(" days left\n");
        b.append("study time logged in the app: ").append(r.minutesLast7Days()).append(" min in the last 7 days, ").append(r.minutesPrevious7Days())
                .append(" min the 7 days before (study done outside the app is not counted)\n");
        List<AiSuggestionRequest.Attempt> all = r.recentAttempts() == null ? List.of() : r.recentAttempts();
        List<AiSuggestionRequest.Attempt> real = all.stream().filter(AiSuggestionService::realAttempt).toList();
        if (real.isEmpty()) b.append("mock attempts: none with a real attempt\n");
        else {
            b.append("recent mock attempts (oldest first):\n");
            for (var a : real) {
                int[] n = answered(a);
                String kind = a.sections() != null && a.sections().size() == 3 ? "full-length"
                        : "sectional, " + String.join("+", a.sections() == null ? List.of() : a.sections().keySet()) + " only (shows that section, not overall level)";
                b.append("- [").append(kind).append("] ").append(clean(a.title())).append(": ").append(a.score()).append("/").append(a.max())
                        .append(", answered ").append(n[0]).append(" of ").append(n[1]);
                if (a.sections() != null) a.sections().forEach((k, s) -> b.append(" | ").append(clean(k)).append(" ").append(s.correct()).append(" right, ")
                        .append(s.wrong()).append(" wrong, ").append(s.unattempted()).append(" skipped, ").append(s.minutes()).append(" min"));
                b.append('\n');
            }
        }
        if (real.size() < all.size()) {
            b.append(all.size() - real.size()).append(" other attempt(s) were left almost blank (under a quarter answered) and say nothing about ability; ignore them\n");
        }
        Set<String> overdue = new LinkedHashSet<>(r.overdueTopicIds() == null ? List.of() : r.overdueTopicIds());
        b.append("\nTOPICS (topicId | section | chapter | name | status | high-yield | revision overdue | questions in bank | solved)\n");
        Map<String, Integer> status = r.topicStatus() == null ? Map.of() : r.topicStatus();
        byId.forEach((id, t) -> {
            var c = bank.get(id);
            b.append(id).append(" | ").append(t.getSection()).append(" | ").append(t.getChapter()).append(" | ").append(t.getName()).append(" | ")
                    .append(switch (status.getOrDefault(id, 0)) { case 2 -> "covered"; case 1 -> "in progress"; default -> "not covered"; })
                    .append(" | ").append(t.isHighYield() ? "high-yield" : "-").append(" | ").append(overdue.contains(id) ? "overdue" : "-")
                    .append(" | ").append(c == null ? 0 : c.questions()).append(" | ").append(c == null ? 0 : c.solved()).append('\n');
        });
        b.append("\nTESTS (mockId | title | kind | sections | attempted)\n");
        mockById.values().forEach(m -> b.append(m.id()).append(" | ").append(m.title()).append(" | ").append(m.kind()).append(" | ")
                .append(m.sections().stream().map(Enum::name).collect(Collectors.joining("+"))).append(" | ")
                .append(attempted.contains(m.id()) || m.progress().attempts() > 0 ? "attempted" : "not attempted").append('\n'));
        return b.toString();
    }

    /** Only text the learner's browser sent that ends up in the prompt: stripped of control characters and line breaks. */
    private static String clean(String s) {
        return s == null ? "" : s.replaceAll("[\\p{Cntrl}]+", " ").trim();
    }

    /* ---- API call ---- */

    private record Reply(String content, String model) {}

    /**
     * Asks the provider. Free tiers rate-limit (HTTP 429): the request is retried once after the wait the provider asks
     * for (at most 8 seconds), then sent to the fallback model, which has its own limit.
     */
    private Reply chat(String system, String user) {
        List<String> models = fallbackModel.isBlank() || fallbackModel.equals(model) ? List.of(model) : List.of(model, fallbackModel);
        for (String m : models) {
            for (int attempt = 0; attempt < 2; attempt++) {
                HttpResponse<String> res = send(m, system, user);
                if (res.statusCode() == 429) {
                    if (attempt == 0) { pause(retryAfter(res)); continue; }
                    log.warn("AI suggestions: {} is rate-limited", m);
                    break;  // next model
                }
                if (res.statusCode() == 401 || res.statusCode() == 403) {
                    log.error("AI suggestions: the provider rejected the API key (HTTP {})", res.statusCode());
                    throw new ServiceUnavailableException("The AI service rejected this server's API key.");
                }
                if (res.statusCode() / 100 != 2) {
                    log.warn("AI suggestions: provider returned HTTP {}: {}", res.statusCode(), abbreviate(res.body()));
                    throw new ServiceUnavailableException("The AI service couldn't answer (HTTP " + res.statusCode() + ").");
                }
                try {
                    return new Reply(json.readTree(res.body()).path("choices").path(0).path("message").path("content").asText(""), m);
                } catch (Exception e) {
                    throw new ServiceUnavailableException("The AI's answer couldn't be read. Try again.");
                }
            }
        }
        throw new ServiceUnavailableException("The AI service is busy right now. Try again in a minute.");
    }

    private HttpResponse<String> send(String m, String system, String user) {
        try {
            String body = json.writeValueAsString(Map.of(
                    "model", m,
                    "temperature", 0.3,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(Map.of("role", "system", "content", system), Map.of("role", "user", "content", user))));
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            return http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("The AI request was interrupted.");
        } catch (Exception e) {
            log.warn("AI suggestions: request failed: {}", e.toString());
            throw new ServiceUnavailableException("Couldn't reach the AI service.");
        }
    }

    /** The provider's requested wait, from Retry-After (seconds), capped at 8 seconds; 2 seconds when not given. */
    static long retryAfter(HttpResponse<String> res) {
        try {
            double sec = Double.parseDouble(res.headers().firstValue("retry-after").orElse("2"));
            return (long) (Math.max(0, Math.min(8, sec)) * 1000);
        } catch (NumberFormatException e) {
            return 2000;
        }
    }

    private static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("The AI request was interrupted.");
        }
    }

    /* ---- checking the answer ---- */

    private AiSuggestionResponse parse(String reply, String usedModel, AiSuggestionRequest req, Map<String, Topic> byId, Map<String, PracticeService.TopicCounts> bank,
                                       Map<String, MockSummary> mockById, Set<String> attempted, Instant now) {
        JsonNode root;
        try {
            String text = reply.strip().replaceAll("^```(?:json)?\\s*|\\s*```$", "");
            root = json.readTree(text);
        } catch (Exception e) {
            log.warn("AI suggestions: reply was not JSON: {}", abbreviate(reply));
            throw new ServiceUnavailableException("The AI's answer couldn't be read. Try again.");
        }
        Map<String, Integer> status = req.topicStatus() == null ? Map.of() : req.topicStatus();
        List<AiSuggestionResponse.TopicPick> picks = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (JsonNode n : root.path("topics")) {
            String id = n.path("topicId").asText("");
            Topic t = byId.get(id);
            if (t == null || !seen.add(id) || picks.size() >= MAX_TOPICS) continue;  // unknown or repeated id: dropped
            var c = bank.get(id);
            String p = n.path("priority").asText("medium").toLowerCase();
            picks.add(new AiSuggestionResponse.TopicPick(id, t.getName(), t.getSection().name(), t.getChapter(),
                    switch (status.getOrDefault(id, 0)) { case 2 -> "covered"; case 1 -> "in progress"; default -> "not covered"; },
                    c == null ? 0 : (int) c.questions(), c == null ? 0 : (int) c.solved(),
                    Set.of("high", "medium", "low").contains(p) ? p : "medium", text(n.path("reason").asText("")), "/practice/" + id));
        }
        List<AiSuggestionResponse.TestPick> tests = new ArrayList<>();
        Set<String> seenTests = new LinkedHashSet<>();
        for (JsonNode n : root.path("tests")) {
            String id = n.path("mockId").asText("");
            MockSummary m = mockById.get(id);
            if (m == null || !seenTests.add(id) || tests.size() >= MAX_TESTS) continue;
            tests.add(new AiSuggestionResponse.TestPick(id, m.title(), m.kind().name(), attempted.contains(id) || m.progress().attempts() > 0,
                    text(n.path("reason").asText("")), "/mocks/start/" + id));
        }
        if (picks.isEmpty() && tests.isEmpty()) throw new ServiceUnavailableException("The AI didn't suggest anything usable. Try again.");
        return new AiSuggestionResponse(text(root.path("summary").asText("")), likelihood(root.path("likelihood"), req), picks, tests, usedModel, now);
    }

    /** The model's estimate, clamped, with the label and (when there is no mock evidence) the confidence set here. */
    private static AiSuggestionResponse.Likelihood likelihood(JsonNode n, AiSuggestionRequest req) {
        if (!n.path("percent").isNumber()) return null;
        int p = Math.max(1, Math.min(99, (int) Math.round(n.path("percent").asDouble())));
        String label = p < 15 ? "Very unlikely" : p < 35 ? "Unlikely" : p < 60 ? "Possible" : p < 80 ? "Likely" : "Very likely";
        String c = n.path("confidence").asText("medium").toLowerCase();
        if (!Set.of("low", "medium", "high").contains(c)) c = "medium";
        if (fullMocks(req).isEmpty()) c = "low";
        Double realistic = n.path("realisticPercentile").isNumber()
                ? Math.round(Math.max(1, Math.min(99.9, n.path("realisticPercentile").asDouble())) * 10) / 10.0 : null;
        return new AiSuggestionResponse.Likelihood(p, label, c, text(n.path("reason").asText("")), realistic);
    }

    /** Full-length mocks the learner actually attempted: the only evidence of their overall level. */
    private static List<AiSuggestionRequest.Attempt> fullMocks(AiSuggestionRequest r) {
        return r.recentAttempts() == null ? List.of()
                : r.recentAttempts().stream().filter(a -> a.sections() != null && a.sections().size() == 3 && a.max() > 0 && realAttempt(a)).toList();
    }

    /** [answered, total] questions in an attempt. */
    private static int[] answered(AiSuggestionRequest.Attempt a) {
        int done = 0, total = 0;
        if (a.sections() != null) for (var s : a.sections().values()) { done += s.correct() + s.wrong(); total += s.correct() + s.wrong() + s.unattempted(); }
        return new int[] {done, total};
    }

    /** A test left almost blank (under a quarter answered) is a try-out, not a measure of ability. */
    static boolean realAttempt(AiSuggestionRequest.Attempt a) {
        int[] n = answered(a);
        return n[1] > 0 && n[0] * 4 >= n[1];
    }

    /** The numbers the likelihood must agree with: the score the target needs, and where recent full mocks put the learner. */
    private String goal(AiSuggestionRequest r) {
        double need = percentiles.requiredRatio(r.targetPercentile());
        StringBuilder b = new StringBuilder("GOAL\n");
        b.append(String.format("target %.1f percentile needs about %.0f%% of the maximum score (about %d/204)%n", r.targetPercentile(), need * 100, Math.round(need * 204)));
        List<AiSuggestionRequest.Attempt> full = fullMocks(r);
        if (full.isEmpty()) b.append("current level: UNKNOWN. There is no full-length mock with a real attempt; do not infer a level from sectional or blank tests\n");
        else {
            double avg = full.stream().mapToDouble(a -> (double) a.score() / a.max()).average().orElse(0);
            b.append(String.format("recent full-length mocks: %d, average %.0f%% of the maximum (about %d/204), which estimates to the %.1f percentile%n",
                    full.size(), avg * 100, Math.round(avg * 204), percentiles.estimate(avg)));
            b.append(String.format("gap to target: about %d marks%n", Math.max(0, Math.round((need - avg) * 204))));
        }
        Map<String, Integer> st = r.topicStatus() == null ? Map.of() : r.topicStatus();
        long covered = st.values().stream().filter(v -> v == 2).count();
        b.append("topics marked covered in the app: ").append(covered).append(" (learners often study before marking topics)\n");
        b.append("days left until CAT ").append(r.targetYear()).append(": ").append(r.daysLeft()).append('\n');
        b.append(timeBudget(r));
        return b.toString();
    }

    /**
     * Study time left against what CAT preparation usually takes. Rough guides only: most aspirants prepare for 6-12 months,
     * and a strong percentile from the basics typically takes several hundred focused hours.
     */
    static String timeBudget(AiSuggestionRequest r) {
        int[] need = r.targetPercentile() >= 99 ? new int[] {700, 900} : r.targetPercentile() >= 95 ? new int[] {500, 700}
                : r.targetPercentile() >= 90 ? new int[] {350, 500} : new int[] {200, 350};
        double paceMin = (r.minutesLast7Days() + r.minutesPrevious7Days()) / 14.0;
        long atPace = Math.round(paceMin * r.daysLeft() / 60);
        long atThree = Math.round(3.0 * r.daysLeft());
        long atFive = Math.round(5.0 * r.daysLeft());
        StringBuilder b = new StringBuilder("TIME\n");
        b.append(String.format("typical preparation for the %.1f percentile, starting from the basics: about %d-%d focused study hours, usually spread over 6-12 months%n",
                r.targetPercentile(), need[0], need[1]));
        b.append(String.format("hours available by exam day: about %d at the pace logged in the app (%.0f min a day), %d at 3 hours a day, %d at 5 hours a day%n",
                atPace, paceMin, atThree, atFive));
        b.append(atFive < need[0] ? "even 5 hours a day falls short of the usual preparation time\n"
                : atThree < need[0] ? "it needs more than 3 hours a day to reach the usual preparation time\n"
                : "there is enough time if they study steadily (about 3 hours a day)\n");
        return b.toString();
    }

    private static String text(String s) {
        String t = s.replaceAll("[\\p{Cntrl}]+", " ").replaceAll("\\s+", " ").trim();
        return t.length() > 400 ? t.substring(0, 400) + "…" : t;
    }

    private static String abbreviate(String s) {
        return s == null ? "" : s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }
}

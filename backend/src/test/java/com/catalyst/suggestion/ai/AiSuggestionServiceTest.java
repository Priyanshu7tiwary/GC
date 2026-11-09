package com.catalyst.suggestion.ai;

import com.catalyst.common.domain.Section;
import com.catalyst.common.error.ServiceUnavailableException;
import com.catalyst.common.error.TooManyRequestsException;
import com.catalyst.mock.AttemptStats;
import com.catalyst.mock.MockCatalogService;
import com.catalyst.mock.MockKind;
import com.catalyst.mock.dto.MockSummary;
import com.catalyst.practice.PracticeService;
import com.catalyst.syllabus.Topic;
import com.catalyst.syllabus.TopicRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiSuggestionServiceTest {
    static final UUID USER = UUID.randomUUID();
    HttpServer server;
    final AtomicReference<String> reply = new AtomicReference<>();
    final AtomicReference<String> sentBody = new AtomicReference<>();
    final AtomicReference<String> sentAuth = new AtomicReference<>();
    final AtomicInteger status = new AtomicInteger(200);
    final AtomicInteger fallbackStatus = new AtomicInteger(200);
    final AtomicInteger calls = new AtomicInteger();
    AiSuggestionService service;
    final java.util.List<AiSuggestionRun> saved = new java.util.ArrayList<>();
    final java.util.concurrent.atomic.AtomicReference<Instant> now = new java.util.concurrent.atomic.AtomicReference<>(Instant.parse("2026-11-08T10:00:00Z"));

    static Topic topic(String id, Section s, String chapter, String name) throws Exception {
        Constructor<Topic> c = Topic.class.getDeclaredConstructor();
        c.setAccessible(true);
        Topic t = c.newInstance();
        for (var kv : List.of(new Object[] {"id", id}, new Object[] {"section", s}, new Object[] {"chapter", chapter}, new Object[] {"name", name})) {
            Field f = Topic.class.getDeclaredField((String) kv[0]);
            f.setAccessible(true);
            f.set(t, kv[1]);
        }
        return t;
    }

    static MockSummary summary(String id, String title) {
        return new MockSummary(id, MockKind.PREVIOUS_YEAR, title, "", 2024, 1, List.of(Section.VARC, Section.DILR, Section.QA), Map.of(), 66, 40, 120, null, List.of(), null, new AttemptStats(0, 0, null, null));
    }

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", ex -> {
            calls.incrementAndGet();
            sentBody.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sentAuth.set(ex.getRequestHeaders().getFirst("Authorization"));
            String content = new ObjectMapper().writeValueAsString(Map.of("choices", List.of(Map.of("message", Map.of("content", reply.get())))));
            int st = sentBody.get().contains("\"fallback-model\"") ? fallbackStatus.get() : status.get();
            byte[] b = (st == 200 ? content : "{\"error\":\"nope\"}").getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.getResponseHeaders().add("Retry-After", "0");
            ex.sendResponseHeaders(st, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        server.start();
        TopicRepository topics = mock(TopicRepository.class);
        when(topics.findAllByOrderByPositionAsc()).thenReturn(List.of(
                topic("qa::arithmetic::percentages", Section.QA, "Arithmetic", "Percentages"),
                topic("varc::rc::inference", Section.VARC, "Reading Comprehension", "Inference")));
        MockCatalogService mocks = mock(MockCatalogService.class);
        when(mocks.list(any(), org.mockito.ArgumentMatchers.eq(MockKind.PREVIOUS_YEAR), any(), any())).thenReturn(List.of(summary("pyq-2024-1", "CAT 2024 · Slot 1")));
        PracticeService practice = mock(PracticeService.class);
        when(practice.summary(any())).thenReturn(Map.of("qa::arithmetic::percentages", new PracticeService.TopicCounts(28, 28, 3, 0)));
        Clock clock = new Clock() {
            public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(java.time.ZoneId z) { return this; }
            public Instant instant() { return now.get(); }
        };
        AiSuggestionRunRepository runs = mock(AiSuggestionRunRepository.class);
        when(runs.save(any())).thenAnswer(i -> { saved.add(i.getArgument(0)); return i.getArgument(0); });
        when(runs.findByUserIdAndCountedTrueAndCreatedAtAfterOrderByCreatedAtAsc(any(), any())).thenAnswer(i -> {
            Instant since = i.getArgument(1);
            return saved.stream().filter(r -> r.isCounted() && r.getCreatedAt().isAfter(since)).toList();
        });
        when(runs.findFirstByUserIdOrderByCreatedAtDesc(any())).thenAnswer(i -> saved.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(saved.get(saved.size() - 1)));
        service = new AiSuggestionService(topics, mocks, practice, runs, estimator(), new ObjectMapper().findAndRegisterModules(), clock, 3, "secret-key", "http://127.0.0.1:" + server.getAddress().getPort(), "test-model", "fallback-model");
    }

    static com.catalyst.attempt.scoring.PercentileEstimator estimator() {
        return new com.catalyst.attempt.scoring.PercentileEstimator() {
            public double estimate(double ratio) { return Math.min(99.9, ratio * 150); }
            public double requiredRatio(double percentile) { return percentile / 200; }
        };
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    AiSuggestionRequest request(int minutes) {
        return new AiSuggestionRequest(99, 2026, 21, Map.of("qa::arithmetic::percentages", 1), List.of(), List.of(), List.of(), minutes, 0);
    }

    @Test
    void keepsOnlyRealTopicsAndTestsAndBuildsTheLinksItself() {
        reply.set("""
                {"summary":"Start with percentages.","likelihood":{"percent":140,"confidence":"high","reason":"Gap of 60 marks."},"topics":[
                  {"topicId":"qa::arithmetic::percentages","priority":"high","reason":"You have solved 3 of 28."},
                  {"topicId":"made::up::topic","priority":"high","reason":"hallucinated"},
                  {"topicId":"qa::arithmetic::percentages","priority":"low","reason":"repeated"},
                  {"topicId":"varc::rc::inference","priority":"urgent!!","reason":"bad priority"}],
                 "tests":[{"mockId":"pyq-2024-1","reason":"Not attempted."},{"mockId":"https://evil.example/x","reason":"link"}]}""");
        var out = service.suggest(USER, request(100)).plan();
        assertThat(out.topics()).extracting(AiSuggestionResponse.TopicPick::topicId).containsExactly("qa::arithmetic::percentages", "varc::rc::inference");
        assertThat(out.topics().get(0).link()).isEqualTo("/practice/qa::arithmetic::percentages");
        assertThat(out.topics().get(0).solved()).isEqualTo(3);
        assertThat(out.topics().get(0).status()).isEqualTo("in progress");
        assertThat(out.topics().get(1).priority()).isEqualTo("medium");
        assertThat(out.tests()).hasSize(1);
        assertThat(out.tests().get(0).link()).isEqualTo("/mocks/start/pyq-2024-1");
        assertThat(out.likelihood().percent()).isEqualTo(99);                // clamped
        assertThat(out.likelihood().label()).isEqualTo("Very likely");       // set by the server, not the model
        assertThat(out.likelihood().confidence()).isEqualTo("low");          // no full-length mocks: never more than low
        assertThat(sentBody.get()).contains("GOAL").contains("current level: UNKNOWN");
        assertThat(sentAuth.get()).isEqualTo("Bearer secret-key");
        assertThat(sentBody.get()).contains("qa::arithmetic::percentages").contains("pyq-2024-1").contains("test-model").doesNotContain("secret-key");
    }

    @Test
    void theGoalNumbersGoToTheModelAndAMissingLikelihoodIsTolerated() {
        reply.set("{\"summary\":\"s\",\"topics\":[{\"topicId\":\"qa::arithmetic::percentages\",\"priority\":\"high\",\"reason\":\"r\"}],\"tests\":[]}");
        var full = new AiSuggestionRequest.Attempt("pyq-2024-1", "CAT 2024", 51, 204, Map.of(
                "VARC", new AiSuggestionRequest.SectionResult(10, 4, 10, 26, 40),
                "DILR", new AiSuggestionRequest.SectionResult(3, 2, 17, 7, 40),
                "QA", new AiSuggestionRequest.SectionResult(7, 3, 12, 18, 40)));
        var req = new AiSuggestionRequest(99, 2026, 20, Map.of(), List.of(full), List.of(), List.of(), 0, 0);
        var out = service.suggest(USER, req).plan();
        assertThat(out.likelihood()).isNull();
        assertThat(sentBody.get()).contains("recent full-length mocks: 1, average 25% of the maximum").contains("gap to target");
    }

    @Test
    void aBlankSectionalIsNotTakenAsTheLearnersLevel() {
        reply.set("{\"summary\":\"s\",\"likelihood\":{\"percent\":20,\"confidence\":\"high\",\"reason\":\"r\"},\"topics\":[{\"topicId\":\"qa::arithmetic::percentages\",\"priority\":\"high\",\"reason\":\"r\"}],\"tests\":[]}");
        var blankQa = new AiSuggestionRequest.Attempt("pyq-2024-1", "CAT 2024 · QA", 0, 66,
                Map.of("QA", new AiSuggestionRequest.SectionResult(0, 0, 22, 0, 1)));
        var realVarc = new AiSuggestionRequest.Attempt("pyq-2024-2", "CAT 2024 · VARC", 40, 72,
                Map.of("VARC", new AiSuggestionRequest.SectionResult(15, 5, 4, 40, 40)));
        var req = new AiSuggestionRequest(99.5, 2027, 384, Map.of(), List.of(blankQa, realVarc), List.of(), List.of(), 0, 0);
        var out = service.suggest(USER, req).plan();
        assertThat(sentBody.get())
                .contains("current level: UNKNOWN")
                .contains("1 other attempt(s) were left almost blank")
                .doesNotContain("CAT 2024 · QA: 0/66")
                .contains("[sectional, VARC only")
                .contains("days left until CAT 2027: 384")
                .contains("about 700-900 focused study hours")
                .contains("1152 at 3 hours a day")
                .contains("there is enough time if they study steadily");
        assertThat(out.likelihood().confidence()).isEqualTo("low");  // no full-length evidence
    }

    @Test
    void aRateLimitedModelIsRetriedOnceThenTheFallbackModelAnswers() {
        status.set(429);
        reply.set(OK);
        var out = service.suggest(USER, request(20)).plan();
        assertThat(calls.get()).isEqualTo(3);                 // main, main again after Retry-After, then fallback
        assertThat(out.model()).isEqualTo("fallback-model");   // the plan says which model wrote it
    }

    @Test
    void whenEveryModelIsRateLimitedTheLearnerIsToldItIsBusyAndKeepsTheirAllowance() {
        status.set(429);
        fallbackStatus.set(429);
        reply.set(OK);
        assertThatThrownBy(() -> service.suggest(USER, request(21))).isInstanceOf(ServiceUnavailableException.class).hasMessageContaining("busy");
        assertThat(service.state(USER).quota().remaining()).isEqualTo(3);
    }

    @Test
    void acceptsAnAnswerWrappedInACodeFence() {
        reply.set("```json\n{\"summary\":\"s\",\"topics\":[{\"topicId\":\"qa::arithmetic::percentages\",\"priority\":\"high\",\"reason\":\"r\"}],\"tests\":[]}\n```");
        assertThat(service.suggest(USER, request(1)).plan().topics()).hasSize(1);
    }

    @Test
    void anAnswerWithNothingUsableIsAnError() {
        reply.set("{\"summary\":\"s\",\"topics\":[{\"topicId\":\"nope\"}],\"tests\":[{\"mockId\":\"nope\"}]}");
        assertThatThrownBy(() -> service.suggest(USER, request(2))).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void textThatIsNotJsonIsAnError() {
        reply.set("Sure! Here are some tips.");
        assertThatThrownBy(() -> service.suggest(USER, request(3))).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void theProviderRejectingTheKeyIsReportedWithoutLeakingIt() {
        status.set(401);
        reply.set("{}");
        assertThatThrownBy(() -> service.suggest(USER, request(4)))
                .isInstanceOf(ServiceUnavailableException.class).hasMessageNotContaining("secret-key");
    }

    static final String OK = "{\"summary\":\"s\",\"topics\":[{\"topicId\":\"qa::arithmetic::percentages\",\"priority\":\"high\",\"reason\":\"r\"}],\"tests\":[]}";

    @Test
    void aLearnerGetsThreePlansAWeekThenWaitsUntilTheOldestAgesOut() {
        reply.set(OK);
        assertThat(service.state(USER).quota().remaining()).isEqualTo(3);
        service.suggest(USER, request(5));
        now.set(now.get().plusSeconds(86400));          // day 2
        service.suggest(USER, request(6));
        now.set(now.get().plusSeconds(86400));          // day 3
        var third = service.suggest(USER, request(7));
        assertThat(third.quota().remaining()).isZero();
        assertThat(third.quota().used()).isEqualTo(3);
        assertThat(third.quota().nextAvailableAt()).isEqualTo(Instant.parse("2026-11-15T10:00:00Z"));  // 7 days after the first
        assertThatThrownBy(() -> service.suggest(USER, request(8))).isInstanceOf(TooManyRequestsException.class).hasMessageContaining("5 days");
        assertThat(calls.get()).isEqualTo(3);
        assertThat(service.state(USER).plan()).isNotNull();  // the last plan is still shown, and looking costs nothing

        now.set(Instant.parse("2026-11-15T10:00:01Z"));  // the first plan has aged out
        assertThat(service.state(USER).quota().remaining()).isEqualTo(1);
        service.suggest(USER, request(9));
        assertThat(calls.get()).isEqualTo(4);
    }

    @Test
    void theAllowanceIsPerLearnerAndAFailedCallCostsNothing() {
        status.set(500);
        reply.set(OK);
        assertThatThrownBy(() -> service.suggest(USER, request(10))).isInstanceOf(ServiceUnavailableException.class);
        assertThat(service.state(USER).quota().remaining()).isEqualTo(3);
        status.set(200);
        service.suggest(USER, request(10));
        assertThat(service.state(USER).quota().remaining()).isEqualTo(2);
    }

    @Test
    void withoutAKeyItIsUnavailableAndMakesNoCall() {
        var off = new AiSuggestionService(mock(TopicRepository.class), mock(MockCatalogService.class), mock(PracticeService.class), mock(AiSuggestionRunRepository.class), estimator(), new ObjectMapper(),
                Clock.systemUTC(), 3, "", "http://127.0.0.1:" + server.getAddress().getPort(), "m", "");
        assertThat(off.available()).isFalse();
        assertThatThrownBy(() -> off.suggest(USER, request(7))).isInstanceOf(ServiceUnavailableException.class);
        assertThat(calls.get()).isZero();
    }
}

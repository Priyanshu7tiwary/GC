package com.catalyst.revision;

import com.catalyst.common.domain.Section;
import com.catalyst.common.error.ConflictException;
import com.catalyst.revision.dto.ReviewResult;
import com.catalyst.revision.dto.ReviewResult.Outcome;
import com.catalyst.syllabus.Topic;
import com.catalyst.syllabus.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RevisionServiceTest {
    static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    static final UUID USER = UUID.randomUUID();
    static final String TOPIC = "qa::arithmetic::percentages";

    /** A clock we can move forward day by day. */
    static class MovableClock extends Clock {
        Instant now = LocalDate.of(2026, 11, 1).atTime(10, 0).atZone(IST).toInstant();
        void plusDays(int d) { now = now.plusSeconds(86400L * d); }
        public ZoneId getZone() { return IST; }
        public Clock withZone(ZoneId z) { return this; }
        public Instant instant() { return now; }
    }

    MovableClock clock;
    RevisionService service;
    RevisionItem[] stored = new RevisionItem[1];
    List<RevisionEvent> log = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        clock = new MovableClock();
        RevisionItemRepository items = mock(RevisionItemRepository.class);
        when(items.lockByUserIdAndTopicId(eq(USER), eq(TOPIC))).thenAnswer(a -> Optional.ofNullable(stored[0]));
        when(items.findByUserIdAndTopicId(eq(USER), eq(TOPIC))).thenAnswer(a -> Optional.ofNullable(stored[0]));
        when(items.save(any(RevisionItem.class))).thenAnswer(a -> stored[0] = a.getArgument(0));
        RevisionEventRepository events = mock(RevisionEventRepository.class);
        when(events.save(any(RevisionEvent.class))).thenAnswer(a -> { log.add(a.getArgument(0)); return a.getArgument(0); });
        when(events.existsByUserIdAndRequestId(eq(USER), anyString())).thenAnswer(a -> log.stream().anyMatch(e -> {
            try {
                Field f = RevisionEvent.class.getDeclaredField("requestId");
                f.setAccessible(true);
                return a.getArgument(1).equals(f.get(e));
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }));
        TopicRepository topics = mock(TopicRepository.class);
        Topic topic = topic();
        when(topics.findById(TOPIC)).thenReturn(Optional.of(topic));
        when(topics.getReferenceById(TOPIC)).thenReturn(topic);
        service = new RevisionService(items, events, topics, new LadderSpacedRepetitionPolicy(), e -> { }, clock, mock(BacklogDayRepository.class));
    }

    static Topic topic() throws Exception {
        Constructor<Topic> c = Topic.class.getDeclaredConstructor();
        c.setAccessible(true);
        Topic t = c.newInstance();
        for (var kv : List.of(new Object[] {"id", TOPIC}, new Object[] {"section", Section.QA}, new Object[] {"chapter", "Arithmetic"}, new Object[] {"name", "Percentages"})) {
            Field f = Topic.class.getDeclaredField((String) kv[0]);
            f.setAccessible(true);
            f.set(t, kv[1]);
        }
        return t;
    }

    LocalDate today() { return LocalDate.now(clock); }

    @Test
    void enrollingSchedulesTomorrowAndIsIdempotent() {
        var v = service.enroll(USER, TOPIC);
        assertThat(v.nextDueOn()).isEqualTo(today().plusDays(1));
        clock.plusDays(1);
        service.review(USER, TOPIC, Rating.GOOD, null);  // stage 1, due in 3 days
        var again = service.enroll(USER, TOPIC);
        assertThat(again.stage()).isEqualTo(1);
        assertThat(again.nextDueOn()).isEqualTo(today().plusDays(3));
    }

    @Test
    void anEarlyReviewIsPracticeAndDoesNotMoveTheSchedule() {
        service.enroll(USER, TOPIC);
        ReviewResult r = service.review(USER, TOPIC, Rating.EASY, null);  // due tomorrow, reviewed today
        assertThat(r.outcome()).isEqualTo(Outcome.PRACTICE_NOT_DUE);
        assertThat(r.item().stage()).isZero();
        assertThat(r.item().nextDueOn()).isEqualTo(today().plusDays(1));
    }

    @Test
    void ratingsFollowTheLadderAndCountFromTheReviewDate() {
        service.enroll(USER, TOPIC);
        clock.plusDays(1);
        assertThat(service.review(USER, TOPIC, Rating.GOOD, null).item().nextDueOn()).isEqualTo(today().plusDays(3));  // 1 → 3
        clock.plusDays(3);
        assertThat(service.review(USER, TOPIC, Rating.EASY, null).item().intervalDays()).isEqualTo(15);                 // 3 → (7) → 15
        clock.plusDays(20);  // five days late
        var late = service.review(USER, TOPIC, Rating.EASY, null).item();
        assertThat(late.intervalDays()).isEqualTo(60);                                                                 // 15 → (30) → 60
        assertThat(late.nextDueOn()).isEqualTo(today().plusDays(60));  // from the review date, not the old due date
        clock.plusDays(60);
        assertThat(service.review(USER, TOPIC, Rating.EASY, null).item().intervalDays()).isEqualTo(60);                 // capped
        clock.plusDays(60);
        var hard = service.review(USER, TOPIC, Rating.HARD, null).item();
        assertThat(hard.stage()).isZero();
        assertThat(hard.nextDueOn()).isEqualTo(today().plusDays(1));
    }

    @Test
    void theScheduleMovesAtMostOncePerDay() {
        service.enroll(USER, TOPIC);
        clock.plusDays(1);
        ReviewResult first = service.review(USER, TOPIC, Rating.GOOD, null);
        ReviewResult second = service.review(USER, TOPIC, Rating.EASY, null);
        assertThat(first.outcome()).isEqualTo(Outcome.ADVANCED);
        assertThat(second.outcome()).isEqualTo(Outcome.PRACTICE_ALREADY_REVIEWED_TODAY);
        assertThat(second.item().stage()).isEqualTo(1);
    }

    @Test
    void aRetriedRequestIsNotAppliedTwice() {
        service.enroll(USER, TOPIC);
        clock.plusDays(1);
        service.review(USER, TOPIC, Rating.GOOD, "req-12345678");
        clock.plusDays(3);  // even once the topic is due again, the same request id is not replayed
        ReviewResult retry = service.review(USER, TOPIC, Rating.GOOD, "req-12345678");
        assertThat(retry.outcome()).isEqualTo(Outcome.DUPLICATE_REQUEST);
        assertThat(retry.item().stage()).isEqualTo(1);
    }

    @Test
    void reopeningPausesWithoutLosingProgressAndResetIsExplicit() {
        service.enroll(USER, TOPIC);
        clock.plusDays(1);
        service.review(USER, TOPIC, Rating.GOOD, null);  // stage 1
        var paused = service.pause(USER, TOPIC);
        assertThat(paused.paused()).isTrue();
        clock.plusDays(10);
        assertThatThrownBy(() -> service.review(USER, TOPIC, Rating.GOOD, null)).isInstanceOf(ConflictException.class);
        var resumed = service.enroll(USER, TOPIC);
        assertThat(resumed.paused()).isFalse();
        assertThat(resumed.stage()).isEqualTo(1);
        assertThat(resumed.nextDueOn()).isEqualTo(today().plusDays(1));  // fell due while paused: due again tomorrow
        var reset = service.reset(USER, TOPIC);
        assertThat(reset.stage()).isZero();
        assertThat(log).extracting(RevisionEvent::getKind).containsExactly(
                RevisionEvent.Kind.ENROLL, RevisionEvent.Kind.REVIEW, RevisionEvent.Kind.PAUSE, RevisionEvent.Kind.RESUME, RevisionEvent.Kind.RESET);
    }

    @Test
    void dueDatesUseTheExamCalendarDateNotUtc() {
        clock.now = LocalDate.of(2026, 11, 1).atTime(0, 30).atZone(IST).toInstant();  // still 31 Oct in UTC
        assertThat(clock.now.atZone(ZoneOffset.UTC).toLocalDate()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(service.enroll(USER, TOPIC).nextDueOn()).isEqualTo(LocalDate.of(2026, 11, 2));
    }
}

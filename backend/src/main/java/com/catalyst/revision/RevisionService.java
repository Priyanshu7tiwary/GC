package com.catalyst.revision;

import com.catalyst.common.error.ConflictException;
import com.catalyst.common.error.NotFoundException;
import com.catalyst.events.StudySessionEvent;
import com.catalyst.events.TopicCompletedEvent;
import com.catalyst.events.TopicReopenedEvent;
import com.catalyst.revision.dto.ReviewResult;
import com.catalyst.revision.dto.RevisionBoard;
import com.catalyst.revision.dto.RevisionEventView;
import com.catalyst.revision.dto.RevisionView;
import com.catalyst.revision.dto.RevisionView.Bucket;
import com.catalyst.syllabus.Topic;
import com.catalyst.syllabus.TopicRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The learner's revision schedule — the single source of truth for due dates and stages.
 *
 * Rules:
 * <ul>
 *   <li>Enrolling (marking a topic learned) schedules the first review for tomorrow. Enrolling again never resets
 *       progress; it only resumes a paused topic.</li>
 *   <li>Only a due or overdue review moves the schedule. Reviewing early is recorded as practice.</li>
 *   <li>The schedule moves at most once per topic per calendar day; further reviews that day are practice.
 *       A retried request (same requestId) is recognised and not applied again.</li>
 *   <li>Hard resets to the first rung, Good climbs one, Easy climbs two (see {@link SpacedRepetitionPolicy}).</li>
 *   <li>The next due date counts from the day of the review, so a late review is not immediately overdue again.</li>
 *   <li>Reopening a topic pauses its schedule and keeps its history; restarting is an explicit reset.</li>
 * </ul>
 * Dates are calendar dates in the exam time zone (the injected clock), so a browser's time zone or daylight-saving
 * change cannot move a due date. Every change is recorded in revision_events.
 */
@Service
public class RevisionService {
    private static final int MINUTES_PER_REVISION = 15;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM");

    private final RevisionItemRepository items;
    private final RevisionEventRepository history;
    private final TopicRepository topics;
    private final SpacedRepetitionPolicy policy;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final BacklogDayRepository backlog;

    public RevisionService(RevisionItemRepository items, RevisionEventRepository history, TopicRepository topics,
                           SpacedRepetitionPolicy policy, ApplicationEventPublisher events, Clock clock, BacklogDayRepository backlog) {
        this.items = items;
        this.history = history;
        this.topics = topics;
        this.policy = policy;
        this.events = events;
        this.clock = clock;
        this.backlog = backlog;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    @Transactional(readOnly = true)
    public List<RevisionView> list(UUID userId) {
        LocalDate today = today();
        Map<String, Topic> byId = topics.findAll().stream().collect(Collectors.toMap(Topic::getId, Function.identity()));
        return items.findByUserIdOrderByNextDueOnAsc(userId).stream().map(i -> view(i, byId.get(i.getTopicId()), today)).toList();
    }

    /** Summary counts, a 14-day forecast and every topic in the schedule. Paused topics are listed but not counted. */
    @Transactional(readOnly = true)
    public RevisionBoard board(UUID userId) {
        LocalDate today = today();
        List<RevisionView> all = list(userId);
        List<RevisionView> active = all.stream().filter(v -> !v.paused()).toList();
        int overdue = count(active, Bucket.OVERDUE), due = count(active, Bucket.DUE_TODAY);
        int week = (int) active.stream().filter(v -> v.bucket() == Bucket.UPCOMING && v.daysUntilDue() <= 7).count();
        int retained = (int) active.stream().filter(v -> v.stage() >= policy.retainedStage()).count();
        List<RevisionBoard.ForecastDay> forecast = IntStream.range(0, 14).mapToObj(i -> {
            LocalDate day = today.plusDays(i);
            int n = (int) active.stream().filter(v -> i == 0 ? !v.nextDueOn().isAfter(day) : v.nextDueOn().equals(day)).count();
            return new RevisionBoard.ForecastDay(day, n);
        }).toList();
        List<Integer> intervals = IntStream.rangeClosed(0, policy.retainedStage() + 1).map(policy::intervalDays).boxed().toList();
        return new RevisionBoard(new RevisionBoard.Summary(overdue, due, week, active.size() - overdue - due, retained), intervals, forecast, all);
    }

    /** Saves today's overdue and due counts for this learner (called when the board is loaded). */
    @Transactional
    public void recordBacklog(UUID userId) {
        LocalDate today = today();
        List<RevisionItem> active = items.findByUserIdOrderByNextDueOnAsc(userId).stream().filter(i -> !i.isPaused()).toList();
        int overdue = (int) active.stream().filter(i -> i.getNextDueOn().isBefore(today)).count();
        int due = (int) active.stream().filter(i -> i.getNextDueOn().equals(today)).count();
        backlog.upsert(userId, today, overdue, due);
    }

    public record BacklogDay(LocalDate day, int overdue, int due) {}

    /** The daily backlog snapshots for the last `days` days, oldest first. */
    @Transactional(readOnly = true)
    public List<BacklogDay> backlogHistory(UUID userId, int days) {
        return backlog.since(userId, today().minusDays(Math.max(1, Math.min(days, 366)))).stream()
                .map(r -> new BacklogDay(r[0] instanceof java.sql.Date d ? d.toLocalDate() : (LocalDate) r[0], ((Number) r[1]).intValue(), ((Number) r[2]).intValue()))
                .toList();
    }

    /** Puts a topic in the schedule, first review tomorrow. Idempotent: an existing topic keeps its progress (and resumes if paused). */
    @Transactional
    public RevisionView enroll(UUID userId, String topicId) {
        Topic topic = topics.findById(topicId).orElseThrow(() -> new NotFoundException("Unknown topic: " + topicId));
        LocalDate today = today();
        RevisionItem item = items.lockByUserIdAndTopicId(userId, topicId).orElse(null);
        if (item == null) {
            int stage = policy.initialStage();
            item = items.save(new RevisionItem(userId, topicId, stage, today, today.plusDays(policy.intervalDays(stage))));
            log(item, RevisionEvent.Kind.ENROLL, null, null, null, null, today, null);
        } else if (item.isPaused()) {
            LocalDate priorDue = item.getNextDueOn();
            item.resume(today);
            log(item, RevisionEvent.Kind.RESUME, null, item.getStage(), item.getStage(), priorDue, today, null);
        }
        return view(item, topic, today);
    }

    /** Enrols several topics at once (e.g. topics already marked learned on another device). Idempotent. */
    @Transactional
    public List<RevisionView> enrollAll(UUID userId, Collection<String> topicIds) {
        return topicIds.stream().distinct().map(id -> enroll(userId, id)).toList();
    }

    @Transactional
    public ReviewResult review(UUID userId, String topicId, Rating rating, String requestId) {
        LocalDate today = today();
        RevisionItem item = items.lockByUserIdAndTopicId(userId, topicId)
                .orElseThrow(() -> new NotFoundException("This topic is not in your revision schedule."));
        Topic topic = topics.getReferenceById(topicId);
        if (requestId != null && history.existsByUserIdAndRequestId(userId, requestId)) {
            return new ReviewResult(view(item, topic, today), ReviewResult.Outcome.DUPLICATE_REQUEST, "Already recorded.");
        }
        if (item.isPaused()) throw new ConflictException("This topic is paused. Mark it learned again to resume its revisions.");

        events.publishEvent(new StudySessionEvent(userId, MINUTES_PER_REVISION));
        if (!item.canAdvance(today)) {
            boolean alreadyToday = today.equals(item.getLastAdvancedOn());
            item.practised(today);
            log(item, RevisionEvent.Kind.PRACTICE, rating, item.getStage(), item.getStage(), item.getNextDueOn(), today, requestId);
            String message = alreadyToday
                    ? "Already reviewed today, so this counts as extra practice. The next review stays on " + item.getNextDueOn().format(DAY) + "."
                    : "Not due until " + item.getNextDueOn().format(DAY) + ", so this counts as practice and the schedule is unchanged.";
            return new ReviewResult(view(item, topic, today), alreadyToday ? ReviewResult.Outcome.PRACTICE_ALREADY_REVIEWED_TODAY
                    : ReviewResult.Outcome.PRACTICE_NOT_DUE, message);
        }
        int priorStage = item.getStage();
        LocalDate priorDue = item.getNextDueOn();
        int stage = policy.nextStage(priorStage, rating);
        item.advance(stage, today, today.plusDays(policy.intervalDays(stage)));  // counted from today, not from the old due date
        log(item, RevisionEvent.Kind.REVIEW, rating, priorStage, stage, priorDue, today, requestId);
        return new ReviewResult(view(item, topic, today), ReviewResult.Outcome.ADVANCED,
                "Next review on " + item.getNextDueOn().format(DAY) + " (in " + policy.intervalDays(stage) + " day" + (policy.intervalDays(stage) == 1 ? "" : "s") + ").");
    }

    /** Stops a topic coming due (it was reopened) while keeping its stage and history. */
    @Transactional
    public RevisionView pause(UUID userId, String topicId) {
        RevisionItem item = require(userId, topicId);
        LocalDate today = today();
        if (!item.isPaused()) {
            item.pause();
            log(item, RevisionEvent.Kind.PAUSE, null, item.getStage(), item.getStage(), item.getNextDueOn(), today, null);
        }
        return view(item, topics.getReferenceById(topicId), today);
    }

    /** Explicit restart: back to the first rung, due tomorrow. History is kept. */
    @Transactional
    public RevisionView reset(UUID userId, String topicId) {
        RevisionItem item = require(userId, topicId);
        LocalDate today = today();
        int priorStage = item.getStage();
        LocalDate priorDue = item.getNextDueOn();
        int stage = policy.initialStage();
        item.reset(stage, today, today.plusDays(policy.intervalDays(stage)));
        log(item, RevisionEvent.Kind.RESET, null, priorStage, stage, priorDue, today, null);
        return view(item, topics.getReferenceById(topicId), today);
    }

    @Transactional(readOnly = true)
    public List<RevisionEventView> history(UUID userId, String topicId) {
        if (items.findByUserIdAndTopicId(userId, topicId).isEmpty()) {  // no row lock: this is a read-only transaction
            throw new NotFoundException("This topic is not in your revision schedule.");
        }
        return history.findByUserIdAndTopicIdOrderByCreatedAtDesc(userId, topicId).stream().map(RevisionEventView::of).toList();
    }

    /* ---- reactions to coverage changes ---- */

    @EventListener
    @Transactional
    public void onTopicCompleted(TopicCompletedEvent e) {
        enroll(e.userId(), e.topicId());
    }

    @EventListener
    @Transactional
    public void onTopicReopened(TopicReopenedEvent e) {
        if (items.findByUserIdAndTopicId(e.userId(), e.topicId()).isPresent()) pause(e.userId(), e.topicId());
    }

    private RevisionItem require(UUID userId, String topicId) {
        return items.lockByUserIdAndTopicId(userId, topicId)
                .orElseThrow(() -> new NotFoundException("This topic is not in your revision schedule."));
    }

    private void log(RevisionItem item, RevisionEvent.Kind kind, Rating rating, Integer priorStage, Integer newStage,
                     LocalDate priorDue, LocalDate today, String requestId) {
        history.save(new RevisionEvent(item.getUserId(), item.getTopicId(), kind, rating, today, priorStage,
                newStage == null ? item.getStage() : newStage, priorDue, item.getNextDueOn(), requestId, clock.instant()));
    }

    private RevisionView view(RevisionItem i, Topic t, LocalDate today) {
        long delta = ChronoUnit.DAYS.between(today, i.getNextDueOn());
        Bucket bucket = i.isPaused() ? Bucket.PAUSED : delta < 0 ? Bucket.OVERDUE : delta == 0 ? Bucket.DUE_TODAY : Bucket.UPCOMING;
        return new RevisionView(i.getTopicId(), t.getName(), t.getSection(), t.getChapter(), i.getStage(),
                policy.intervalDays(i.getStage()), i.getEnrolledOn(), i.getLastRevisedOn(), i.getNextDueOn(), delta, i.getReviewCount(),
                bucket, i.isPaused(), i.canAdvance(today));
    }

    private static int count(List<RevisionView> all, Bucket b) {
        return (int) all.stream().filter(v -> v.bucket() == b).count();
    }
}

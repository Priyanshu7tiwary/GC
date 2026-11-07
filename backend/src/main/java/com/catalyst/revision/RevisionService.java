package com.catalyst.revision;

import com.catalyst.common.error.NotFoundException;
import com.catalyst.events.StudySessionEvent;
import com.catalyst.events.TopicCompletedEvent;
import com.catalyst.events.TopicReopenedEvent;
import com.catalyst.revision.dto.RevisionBoard;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class RevisionService {
    private static final int MINUTES_PER_REVISION = 15;

    private final RevisionItemRepository items;
    private final TopicRepository topics;
    private final SpacedRepetitionPolicy policy;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RevisionService(RevisionItemRepository items, TopicRepository topics, SpacedRepetitionPolicy policy,
                           ApplicationEventPublisher events, Clock clock) {
        this.items = items;
        this.topics = topics;
        this.policy = policy;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RevisionView> list(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        Map<String, Topic> byId = topics.findAll().stream().collect(Collectors.toMap(Topic::getId, Function.identity()));
        return items.findByUserIdOrderByNextDueOnAsc(userId).stream().map(i -> view(i, byId.get(i.getTopicId()), today)).toList();
    }

    @Transactional(readOnly = true)
    public RevisionBoard board(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        List<RevisionView> all = list(userId);
        int overdue = count(all, Bucket.OVERDUE), due = count(all, Bucket.DUE_TODAY);
        int week = (int) all.stream().filter(v -> v.bucket() == Bucket.UPCOMING && v.daysUntilDue() <= 7).count();
        int retained = (int) all.stream().filter(v -> v.stage() >= policy.retainedStage()).count();
        List<RevisionBoard.ForecastDay> forecast = IntStream.range(0, 14).mapToObj(i -> {
            LocalDate day = today.plusDays(i);
            int n = (int) all.stream().filter(v -> i == 0 ? !v.nextDueOn().isAfter(day) : v.nextDueOn().equals(day)).count();
            return new RevisionBoard.ForecastDay(day, n);
        }).toList();
        List<Integer> intervals = IntStream.rangeClosed(0, policy.retainedStage() + 1).map(policy::intervalDays).boxed().toList();
        return new RevisionBoard(new RevisionBoard.Summary(overdue, due, week, all.size() - overdue - due, retained), intervals, forecast, all);
    }

    /** Start a topic's revision cycle today (studied it just now). */
    @Transactional
    public RevisionView enroll(UUID userId, String topicId) {
        Topic topic = topics.findById(topicId).orElseThrow(() -> new NotFoundException("Unknown topic: " + topicId));
        LocalDate today = LocalDate.now(clock);
        RevisionItem item = items.findByUserIdAndTopicId(userId, topicId).orElseGet(() -> {
            int stage = policy.initialStage();
            return items.save(new RevisionItem(userId, topicId, stage, today, today.plusDays(policy.intervalDays(stage))));
        });
        return view(item, topic, today);
    }

    @Transactional
    public RevisionView review(UUID userId, String topicId, Rating rating) {
        RevisionItem item = items.findByUserIdAndTopicId(userId, topicId)
                .orElseThrow(() -> new NotFoundException("This topic is not in your revision schedule."));
        LocalDate today = LocalDate.now(clock);
        int stage = policy.nextStage(item.getStage(), rating);
        item.recordReview(stage, today, today.plusDays(policy.intervalDays(stage)));
        events.publishEvent(new StudySessionEvent(userId, MINUTES_PER_REVISION));
        return view(item, topics.getReferenceById(topicId), today);
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
        items.deleteByUserIdAndTopicId(e.userId(), e.topicId());
    }

    private RevisionView view(RevisionItem i, Topic t, LocalDate today) {
        long delta = ChronoUnit.DAYS.between(today, i.getNextDueOn());
        Bucket bucket = delta < 0 ? Bucket.OVERDUE : delta == 0 ? Bucket.DUE_TODAY : Bucket.UPCOMING;
        return new RevisionView(i.getTopicId(), t.getName(), t.getSection(), t.getChapter(), i.getStage(),
                policy.intervalDays(i.getStage()), i.getLastRevisedOn(), i.getNextDueOn(), delta, i.getReviewCount(), bucket);
    }

    private static int count(List<RevisionView> all, Bucket b) {
        return (int) all.stream().filter(v -> v.bucket() == b).count();
    }
}

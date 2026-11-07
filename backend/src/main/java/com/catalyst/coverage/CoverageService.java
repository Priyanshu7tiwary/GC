package com.catalyst.coverage;

import com.catalyst.common.domain.Section;
import com.catalyst.common.error.NotFoundException;
import com.catalyst.coverage.dto.CoverageView;
import com.catalyst.coverage.dto.CoverageView.Stats;
import com.catalyst.coverage.dto.CoverageView.TopicCoverage;
import com.catalyst.coverage.dto.UpdateCoverageRequest;
import com.catalyst.events.TopicCompletedEvent;
import com.catalyst.events.TopicReopenedEvent;
import com.catalyst.syllabus.Topic;
import com.catalyst.syllabus.TopicRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CoverageService {
    private final TopicRepository topics;
    private final TopicProgressRepository progress;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public CoverageService(TopicRepository topics, TopicProgressRepository progress, ApplicationEventPublisher events, Clock clock) {
        this.topics = topics;
        this.progress = progress;
        this.events = events;
        this.clock = clock;
    }

    /** Every topic merged with the learner's progress; topics never touched read as NOT_STARTED. */
    @Transactional(readOnly = true)
    public List<TopicCoverage> topicsFor(UUID userId) {
        Map<String, TopicProgress> mine = progress.findByUserId(userId).stream()
                .collect(Collectors.toMap(TopicProgress::getTopicId, Function.identity()));
        return topics.findAllByOrderByPositionAsc().stream().map(t -> {
            TopicProgress p = mine.get(t.getId());
            return new TopicCoverage(t.getId(), t.getSection(), t.getChapter(), t.getName(), t.isHighYield(),
                    p == null ? CoverageStatus.NOT_STARTED : p.getStatus(), p == null ? 0 : p.getPracticed());
        }).toList();
    }

    @Transactional(readOnly = true)
    public CoverageView view(UUID userId) {
        List<TopicCoverage> all = topicsFor(userId);
        Map<Section, Stats> bySection = new EnumMap<>(Section.class);
        for (Section s : Section.values()) {
            bySection.put(s, stats(all.stream().filter(t -> t.section() == s).toList()));
        }
        return new CoverageView(stats(all), bySection, all);
    }

    @Transactional
    public TopicCoverage update(UUID userId, String topicId, UpdateCoverageRequest req) {
        Topic topic = topics.findById(topicId).orElseThrow(() -> new NotFoundException("Unknown topic: " + topicId));
        var now = clock.instant();
        TopicProgress p = progress.findByUserIdAndTopicId(userId, topicId)
                .orElseGet(() -> progress.save(new TopicProgress(userId, topicId, now)));
        CoverageStatus before = p.getStatus();

        if (req.practiced() != null) p.setPracticed(req.practiced(), now);
        if (req.practicedDelta() != null) p.setPracticed(p.getPracticed() + req.practicedDelta(), now);
        if (req.status() != null) p.changeStatus(req.status(), now);

        if (before != CoverageStatus.COMPLETED && p.getStatus() == CoverageStatus.COMPLETED) {
            events.publishEvent(new TopicCompletedEvent(userId, topicId));
        } else if (before == CoverageStatus.COMPLETED && p.getStatus() != CoverageStatus.COMPLETED) {
            events.publishEvent(new TopicReopenedEvent(userId, topicId));
        }
        return new TopicCoverage(topic.getId(), topic.getSection(), topic.getChapter(), topic.getName(),
                topic.isHighYield(), p.getStatus(), p.getPracticed());
    }

    public static Stats stats(Collection<TopicCoverage> list) {
        int done = (int) list.stream().filter(t -> t.status() == CoverageStatus.COMPLETED).count();
        int prog = (int) list.stream().filter(t -> t.status() == CoverageStatus.IN_PROGRESS).count();
        double weighted = list.stream().mapToDouble(t -> t.status().weight()).sum();
        int pct = list.isEmpty() ? 0 : (int) Math.round(weighted / list.size() * 100);
        int practiced = list.stream().mapToInt(TopicCoverage::practiced).sum();
        return new Stats(list.size(), done, prog, pct, practiced);
    }
}

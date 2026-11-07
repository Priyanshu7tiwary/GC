package com.catalyst.attempt;

import com.catalyst.attempt.dto.AttemptDetail;
import com.catalyst.attempt.dto.AttemptSummary;
import com.catalyst.attempt.dto.SubmitAttemptRequest;
import com.catalyst.attempt.scoring.AnswerChecker;
import com.catalyst.attempt.scoring.MarkingScheme;
import com.catalyst.attempt.scoring.Outcome;
import com.catalyst.attempt.scoring.PercentileEstimator;
import com.catalyst.attempt.scoring.ScoreCard;
import com.catalyst.attempt.scoring.Scorer;
import com.catalyst.common.domain.Section;
import com.catalyst.common.error.NotFoundException;
import com.catalyst.events.StudySessionEvent;
import com.catalyst.mock.Mock;
import com.catalyst.mock.MockCatalogService;
import com.catalyst.mock.QuestionSetResolver;
import com.catalyst.question.Question;
import com.catalyst.question.QuestionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AttemptService {
    private final AttemptRepository attempts;
    private final MockCatalogService catalog;
    private final QuestionSetResolver resolver;
    private final QuestionRepository questions;
    private final Scorer scorer;
    private final AnswerChecker checker;
    private final MarkingScheme marking;
    private final PercentileEstimator percentiles;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public AttemptService(AttemptRepository attempts, MockCatalogService catalog, QuestionSetResolver resolver,
                          QuestionRepository questions, Scorer scorer, AnswerChecker checker, MarkingScheme marking,
                          PercentileEstimator percentiles, ApplicationEventPublisher events, Clock clock) {
        this.attempts = attempts;
        this.catalog = catalog;
        this.resolver = resolver;
        this.questions = questions;
        this.scorer = scorer;
        this.checker = checker;
        this.marking = marking;
        this.percentiles = percentiles;
        this.events = events;
        this.clock = clock;
    }

    /** Scores the submission on the server — the client never sees the answer key until after this. */
    @Transactional
    public AttemptDetail submit(UUID userId, String mockId, SubmitAttemptRequest req) {
        Mock mock = catalog.require(userId, mockId);
        Map<Section, List<Question>> paper = resolver.resolve(mock);
        List<String> ids = paper.values().stream().flatMap(List::stream).map(Question::getId).toList();
        Set<String> known = Set.copyOf(ids);
        Map<String, String> answers = new HashMap<>();
        req.answers().forEach((k, v) -> { if (known.contains(k) && v != null && !v.isBlank()) answers.put(k, v.trim()); });
        Map<Section, Integer> seconds = req.sectionSeconds() == null ? Map.of() : req.sectionSeconds();

        ScoreCard card = scorer.score(paper, answers, seconds);
        Double percentile = mock.isFullLength() && card.maxScore() > 0
                ? round1(percentiles.estimate((double) card.score() / card.maxScore())) : null;
        int timeSpent = seconds.values().stream().mapToInt(Integer::intValue).sum();

        Attempt saved = attempts.save(new Attempt(userId, mock.getId(), mock.getTitle(), mock.isFullLength(), clock.instant(),
                card.score(), card.maxScore(), percentile, timeSpent, card.sections(), answers, ids));
        events.publishEvent(new StudySessionEvent(userId, Math.max(1, timeSpent / 60)));
        return detail(saved);
    }

    @Transactional(readOnly = true)
    public List<AttemptSummary> list(UUID userId) {
        return attempts.findByUserIdOrderBySubmittedAtAsc(userId).stream().map(AttemptSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public List<Attempt> history(UUID userId) {
        return attempts.findByUserIdOrderBySubmittedAtAsc(userId);
    }

    @Transactional(readOnly = true)
    public AttemptDetail get(UUID userId, UUID attemptId) {
        return detail(attempts.findByIdAndUserId(attemptId, userId).orElseThrow(() -> new NotFoundException("Attempt not found.")));
    }

    private AttemptDetail detail(Attempt a) {
        Map<String, Question> byId = questions.findByIdIn(a.getQuestionIds()).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        List<AttemptDetail.Solution> solutions = a.getQuestionIds().stream().map(byId::get).filter(java.util.Objects::nonNull).map(q -> {
            String response = a.getAnswers().get(q.getId());
            Outcome o = checker.check(q, response);
            return new AttemptDetail.Solution(q.getId(), q.getSection(), q.getTopic(), q.getType(), q.getPrompt(),
                    q.getPassage(), q.getOptions(), q.getAnswer(), response, o, marking.marks(q.getType(), o), q.getExplanation());
        }).toList();
        return new AttemptDetail(AttemptSummary.of(a), solutions);
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}

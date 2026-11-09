package com.catalyst.practice;

import com.catalyst.attempt.scoring.AnswerChecker;
import com.catalyst.attempt.scoring.Outcome;
import com.catalyst.common.domain.Section;
import com.catalyst.common.error.BadRequestException;
import com.catalyst.common.error.NotFoundException;
import com.catalyst.question.Difficulty;
import com.catalyst.question.Question;
import com.catalyst.question.QuestionOrigin;
import com.catalyst.question.QuestionRepository;
import com.catalyst.question.QuestionType;
import com.catalyst.syllabus.Topic;
import com.catalyst.syllabus.TopicRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

/**
 * Topic practice: every question in a topic's bank, with the learner's own state (solved, marked for revision).
 * The answer key and explanation of a question are only sent once the learner has checked an answer or marked it
 * solved, so the list can be browsed without spoiling the questions.
 */
@Service
public class PracticeService {
    private static final Set<String> FILTERS = Set.of("all", "unsolved", "solved", "marked");

    private final QuestionPracticeRepository practice;
    private final QuestionRepository questions;
    private final TopicRepository topics;
    private final AnswerChecker checker;
    private final Clock clock;

    public PracticeService(QuestionPracticeRepository practice, QuestionRepository questions, TopicRepository topics,
                           AnswerChecker checker, Clock clock) {
        this.practice = practice;
        this.questions = questions;
        this.topics = topics;
        this.checker = checker;
        this.clock = clock;
    }

    public record TopicCounts(long questions, long keyed, long solved, long marked) {}

    public record State(boolean solved, boolean marked, int attempts, String lastAnswer, Boolean lastCorrect) {
        static final State NONE = new State(false, false, 0, null, null);
        static State of(QuestionPractice p) {
            return p == null ? NONE : new State(p.isSolved(), p.isMarked(), p.getAttempts(), p.getLastAnswer(), p.getLastCorrect());
        }
    }

    public record PracticeQuestion(String id, String label, Section section, QuestionType type, QuestionOrigin origin, Difficulty difficulty,
                                   String paperId, Integer questionNumber, String passage, String prompt, List<String> options,
                                   boolean hasFigure, boolean keyed, State state, String correctAnswer, String explanation) {}

    public record PracticePage(String topicId, String topic, Section section, String chapter, TopicCounts counts,
                               List<PracticeQuestion> items, long total, int page, int size) {}

    public record CheckResult(boolean correct, String correctAnswer, String explanation, State state) {}

    /** Bank size and the learner's solved / marked counts for every topic that has questions. */
    @Transactional(readOnly = true)
    public Map<String, TopicCounts> summary(UUID userId) {
        Map<String, long[]> mine = new HashMap<>();
        for (Object[] r : practice.countsByTopic(userId)) mine.put((String) r[0], new long[]{n(r[1]), n(r[2])});
        Map<String, TopicCounts> out = new HashMap<>();
        for (Object[] r : practice.bankSizes()) {
            long[] m = mine.getOrDefault((String) r[0], new long[2]);
            out.put((String) r[0], new TopicCounts(n(r[1]), n(r[2]), m[0], m[1]));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public PracticePage topic(UUID userId, String topicId, String filter, int page, int size) {
        Topic topic = topics.findById(topicId).orElseThrow(() -> new NotFoundException("Unknown topic: " + topicId));
        if (!FILTERS.contains(filter)) throw new BadRequestException("filter must be one of " + FILTERS);
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 50));
        Page<Question> result = questions.topicQuestions(userId, topicId, filter, pageable);
        Map<String, QuestionPractice> state = practice.findByUserIdAndQuestionIdIn(userId, result.getContent().stream().map(Question::getId).toList())
                .stream().collect(Collectors.toMap(QuestionPractice::getQuestionId, Function.identity()));
        List<PracticeQuestion> items = result.getContent().stream().map(q -> view(q, state.get(q.getId()))).toList();
        TopicCounts counts = summary(userId).getOrDefault(topicId, new TopicCounts(0, 0, 0, 0));
        return new PracticePage(topicId, topic.getName(), topic.getSection(), topic.getChapter(), counts, items,
                result.getTotalElements(), pageable.getPageNumber(), pageable.getPageSize());
    }

    /** Checks an answer. A correct answer marks the question solved; either way the key and explanation are returned. */
    @Transactional
    public CheckResult check(UUID userId, String questionId, String answer) {
        Question q = question(questionId);
        if (!q.isScored()) throw new BadRequestException("This question has no answer key yet, so it can't be checked. You can still mark it solved.");
        Outcome outcome = checker.check(q, answer);
        if (outcome == Outcome.SKIPPED) throw new BadRequestException("Enter an answer first.");
        QuestionPractice p = row(userId, questionId);
        p.checked(answer.trim(), outcome == Outcome.CORRECT, clock.instant());
        return new CheckResult(outcome == Outcome.CORRECT, q.getAnswer(), q.getExplanation(), State.of(p));
    }

    /** Sets solved and/or marked-for-revision (null leaves a flag as it is). */
    @Transactional
    public PracticeQuestion mark(UUID userId, String questionId, Boolean solved, Boolean marked) {
        Question q = question(questionId);
        QuestionPractice p = row(userId, questionId);
        if (solved != null) p.setSolved(solved, clock.instant());
        if (marked != null) p.setMarked(marked, clock.instant());
        return view(q, p);
    }

    private QuestionPractice row(UUID userId, String questionId) {
        return practice.findByUserIdAndQuestionId(userId, questionId)
                .orElseGet(() -> practice.save(new QuestionPractice(userId, questionId, clock.instant())));
    }

    private Question question(String id) {
        return questions.findById(id).orElseThrow(() -> new NotFoundException("Question not found: " + id));
    }

    private static PracticeQuestion view(Question q, QuestionPractice p) {
        State s = State.of(p);
        boolean reveal = s.solved() || s.attempts() > 0;
        return new PracticeQuestion(q.getId(), q.getTopicLabel(), q.getSection(), q.getType(), q.getOrigin(), q.getDifficulty(),
                q.getPaperId(), q.getQuestionNumber(), q.getPassage(), q.getPrompt(), q.getOptions(), q.hasFigure(), q.isScored(), s,
                reveal ? q.getAnswer() : null, reveal ? q.getExplanation() : null);
    }

    private static long n(Object o) {
        return o == null ? 0 : ((Number) o).longValue();
    }
}

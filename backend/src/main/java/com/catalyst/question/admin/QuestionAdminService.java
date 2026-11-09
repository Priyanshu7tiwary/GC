package com.catalyst.question.admin;

import com.catalyst.common.domain.Section;
import com.catalyst.common.error.BadRequestException;
import com.catalyst.common.error.ForbiddenException;
import com.catalyst.common.error.NotFoundException;
import com.catalyst.syllabus.Topic;
import com.catalyst.syllabus.TopicRepository;
import com.catalyst.question.Difficulty;
import com.catalyst.question.Question;
import com.catalyst.question.QuestionOrigin;
import com.catalyst.question.QuestionRepository;
import com.catalyst.question.QuestionType;
import com.catalyst.user.AdminPolicy;
import com.catalyst.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class QuestionAdminService {
    private static final Pattern TITA = Pattern.compile("-?\\d+(\\.\\d+)?");

    private final QuestionRepository questions;
    private final UserRepository users;
    private final AdminPolicy admins;
    private final Clock clock;
    private final TopicRepository topics;

    public QuestionAdminService(QuestionRepository questions, UserRepository users, AdminPolicy admins, Clock clock, TopicRepository topics) {
        this.questions = questions;
        this.topics = topics;
        this.users = users;
        this.admins = admins;
        this.clock = clock;
    }

    public record ImportResult(int added, int skippedDuplicates, List<String> errors) {}

    public record PoolStats(Map<String, Map<String, Long>> keyedBySectionAndOrigin, long total) {}

    /** One topic's question bank. topicId is null for a section's unassigned questions. */
    public record TopicBank(String topicId, Section section, String chapter, String topic, String label, boolean highYield,
                            long questions, long keyed, Map<String, Long> byOrigin) {}

    public record QuestionRow(String id, String label, Section section, String topicId, String topic, QuestionType type,
                              QuestionOrigin origin, Difficulty difficulty, boolean keyed, String paperId, Integer questionNumber, String prompt) {
        static QuestionRow of(Question q) {
            String p = q.getPrompt().replaceAll("\\[\\[img:[^\\]]+\\]\\]", "[figure]").replaceAll("\\s+", " ").trim();
            return new QuestionRow(q.getId(), q.getTopicLabel(), q.getSection(), q.getTopicId(), q.getTopic(), q.getType(), q.getOrigin(),
                    q.getDifficulty(), q.isScored(), q.getPaperId(), q.getQuestionNumber(), p.length() > 240 ? p.substring(0, 240) + "…" : p);
        }
    }

    public record QuestionPage(List<QuestionRow> items, long total, int page, int size) {}

    /** Validates every row first; nothing is saved if any row is invalid. Exact duplicates of stored prompts are skipped. */
    @Transactional
    public ImportResult importQuestions(UUID adminId, List<ImportQuestion> rows) {
        requireAdmin(adminId);
        List<String> errors = new ArrayList<>();
        List<Topic> resolved = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            ImportQuestion r = rows.get(i);
            String problem = problem(r);
            Topic topic = topics.findBySectionAndNameIgnoreCase(r.section(), r.topic().trim()).orElse(null);
            if (problem == null && topic == null) {
                problem = "\"" + r.topic().trim() + "\" isn't a " + r.section() + " syllabus topic. Use one of: "
                        + topics.findAllByOrderByPositionAsc().stream().filter(t -> t.getSection() == r.section()).map(Topic::getName).collect(Collectors.joining(", "));
            }
            if (problem != null) errors.add("Row " + (i + 1) + ": " + problem);
            resolved.add(topic);
        }
        if (!errors.isEmpty()) return new ImportResult(0, 0, errors);

        int position = questions.maxPosition();
        int added = 0, skipped = 0;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            ImportQuestion r = rows.get(i);
            String prompt = r.prompt().trim();
            if (!seen.add(r.section() + "|" + prompt) || questions.existsByPromptAndSection(prompt, r.section())) {
                skipped++;
                continue;
            }
            questions.save(Question.create("imp-" + UUID.randomUUID(), QuestionOrigin.IMPORTED, r.section(), resolved.get(i), r.topic().trim(), r.type(),
                    prompt, r.passage() == null || r.passage().isBlank() ? null : r.passage().trim(),
                    r.type() == QuestionType.MCQ ? r.options().stream().map(String::trim).toList() : List.of(),
                    r.answer().trim(), r.explanation() == null || r.explanation().isBlank() ? "Added by an admin." : r.explanation().trim(),
                    r.difficulty() == null ? Difficulty.MODERATE : r.difficulty(), ++position, clock.instant()));
            added++;
        }
        return new ImportResult(added, skipped, List.of());
    }

    @Transactional(readOnly = true)
    public PoolStats stats(UUID adminId) {
        requireAdmin(adminId);
        Map<String, Map<String, Long>> out = new LinkedHashMap<>();
        long total = 0;
        for (Object[] row : questions.countKeyedBySectionAndOrigin()) {
            out.computeIfAbsent(row[0].toString(), k -> new LinkedHashMap<>()).put(row[1].toString(), (Long) row[2]);
            total += (Long) row[2];
        }
        return new PoolStats(out, total);
    }

    /** Every syllabus topic's bank, plus each section's unassigned questions, in syllabus order. */
    @Transactional(readOnly = true)
    public List<TopicBank> banks(UUID adminId) {
        requireAdmin(adminId);
        Map<String, long[]> totals = new LinkedHashMap<>();
        Map<String, Map<String, Long>> origins = new LinkedHashMap<>();
        for (Object[] row : questions.countByTopicAndOrigin()) {
            String key = row[1] == null ? "unassigned:" + row[0] : (String) row[1];
            long[] t = totals.computeIfAbsent(key, k -> new long[2]);
            t[0] += (Long) row[3];
            t[1] += (Long) row[4];
            origins.computeIfAbsent(key, k -> new LinkedHashMap<>()).merge(row[2].toString(), (Long) row[3], Long::sum);
        }
        List<TopicBank> out = new ArrayList<>();
        for (Topic t : topics.findAllByOrderByPositionAsc()) {
            long[] n = totals.getOrDefault(t.getId(), new long[2]);
            out.add(new TopicBank(t.getId(), t.getSection(), t.getChapter(), t.getName(), Question.label(t), t.isHighYield(),
                    n[0], n[1], origins.getOrDefault(t.getId(), Map.of())));
        }
        for (Section s : Section.values()) {
            long[] n = totals.get("unassigned:" + s);
            if (n != null) out.add(new TopicBank(null, s, "Unassigned", "Unassigned", s + " › Unassigned", false, n[0], n[1], origins.get("unassigned:" + s)));
        }
        return out;
    }

    /** One bank's questions; topicId "unassigned" with a section lists that section's unassigned questions. */
    @Transactional(readOnly = true)
    public QuestionPage questionsIn(UUID adminId, String topicId, Section section, int page, int size) {
        requireAdmin(adminId);
        var pageable = org.springframework.data.domain.PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100));
        var result = "unassigned".equals(topicId)
                ? questions.findBySectionAndTopicIdIsNullOrderByPositionAsc(section == null ? Section.QA : section, pageable)
                : questions.findByTopicIdOrderByPositionAsc(topicId, pageable);
        return new QuestionPage(result.getContent().stream().map(QuestionRow::of).toList(), result.getTotalElements(), pageable.getPageNumber(), pageable.getPageSize());
    }

    /** Moves a question to another topic's bank (within its section). */
    @Transactional
    public QuestionRow moveToTopic(UUID adminId, String questionId, String topicId) {
        requireAdmin(adminId);
        Question q = questions.findById(questionId).orElseThrow(() -> new NotFoundException("Question not found: " + questionId));
        Topic t = topics.findById(topicId).orElseThrow(() -> new BadRequestException("Unknown topic: " + topicId));
        if (t.getSection() != q.getSection()) throw new BadRequestException("That topic is in " + t.getSection() + "; this question is " + q.getSection() + ".");
        q.assignTopic(t);
        return QuestionRow.of(q);
    }

    private static String problem(ImportQuestion r) {
        String answer = r.answer().trim();
        if (r.type() == QuestionType.MCQ) {
            if (r.options() == null || r.options().size() != 4) return "an MCQ needs exactly 4 options";
            Set<String> distinct = new HashSet<>();
            for (String o : r.options()) if (!distinct.add(o.trim().toLowerCase(Locale.ROOT))) return "MCQ options must be distinct";
            try {
                int k = Integer.parseInt(answer);
                if (k < 0 || k > 3) return "an MCQ answer is the option index 0–3";
            } catch (NumberFormatException e) {
                return "an MCQ answer is the option index 0–3";
            }
        } else {
            if (r.options() != null && !r.options().isEmpty()) return "a TITA question has no options";
            if (!TITA.matcher(answer).matches()) return "a TITA answer must be a number, such as 42, 3.5 or 2413";
        }
        return null;
    }

    private void requireAdmin(UUID userId) {
        var user = users.findById(userId).orElseThrow(() -> new ForbiddenException("Admins only."));
        if (!admins.isAdmin(user)) throw new ForbiddenException("Admins only.");
    }
}

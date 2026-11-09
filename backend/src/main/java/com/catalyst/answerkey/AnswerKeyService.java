package com.catalyst.answerkey;

import com.catalyst.answerkey.dto.AdminKeyView;
import com.catalyst.answerkey.dto.AnswerKeyView;
import com.catalyst.common.error.BadRequestException;
import com.catalyst.common.error.ForbiddenException;
import com.catalyst.common.error.NotFoundException;
import com.catalyst.question.Question;
import com.catalyst.question.QuestionRepository;
import com.catalyst.question.QuestionType;
import com.catalyst.user.AdminPolicy;
import com.catalyst.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AnswerKeyService {
    /** What a CAT TITA answer can look like: a number, or a sentence sequence such as 2413. */
    private static final Pattern TITA = Pattern.compile("-?\\d+(\\.\\d+)?");

    private final QuestionRepository questions;
    private final AnswerKeyCorrectionRepository corrections;
    private final UserRepository users;
    private final AdminPolicy admins;
    private final Clock clock;

    public AnswerKeyService(QuestionRepository questions, AnswerKeyCorrectionRepository corrections, UserRepository users,
                            AdminPolicy admins, Clock clock) {
        this.questions = questions;
        this.corrections = corrections;
        this.users = users;
        this.admins = admins;
        this.clock = clock;
    }

    /** Current keys for the corrected questions of one paper, so clients can apply them over the bundled paper. */
    @Transactional(readOnly = true)
    public List<AnswerKeyView> corrections(String paperId) {
        List<String> ids = corrections.correctedQuestionIds(paperId);
        return questions.findByIdIn(ids).stream().map(q -> new AnswerKeyView(q.getId(), q.getAnswer())).toList();
    }

    /** Admin only: every question's current key in one previous-year paper, so admin mode can show and change them. */
    @Transactional(readOnly = true)
    public List<AdminKeyView> keysFor(UUID adminId, String paperId) {
        requireAdmin(adminId);
        java.util.Set<String> corrected = java.util.Set.copyOf(corrections.correctedQuestionIds(paperId));
        return questions.findByPaperIdOrderByPositionAsc(paperId).stream()
                .map(q -> new AdminKeyView(q.getId(), q.getAnswer(), corrected.contains(q.getId()))).toList();
    }

    private void requireAdmin(UUID adminId) {
        var admin = users.findById(adminId).orElseThrow(() -> new ForbiddenException("Only admins can see answer keys before submitting."));
        if (!admins.isAdmin(admin)) throw new ForbiddenException("Only admins can see answer keys before submitting.");
    }

    @Transactional
    public AnswerKeyView correct(UUID adminId, String questionId, String rawAnswer) {
        var admin = users.findById(adminId).orElseThrow(() -> new ForbiddenException("Only admins can change answer keys."));
        if (!admins.isAdmin(admin)) throw new ForbiddenException("Only admins can change answer keys.");
        Question q = questions.findById(questionId).orElseThrow(() -> new NotFoundException("Question not found: " + questionId));
        if (q.getPaperId() == null) throw new BadRequestException("Only previous-year paper questions can be corrected here.");

        String answer = normalize(q, rawAnswer);
        String previous = q.getAnswer();
        if (answer.equals(previous)) return new AnswerKeyView(q.getId(), answer);
        q.replaceAnswer(answer);
        corrections.save(new AnswerKeyCorrection(q.getId(), previous, answer, adminId, clock.instant()));
        return new AnswerKeyView(q.getId(), answer);
    }

    private static String normalize(Question q, String raw) {
        String v = raw.trim();
        if (q.getType() == QuestionType.MCQ) {
            int options = q.getOptions() == null ? 0 : q.getOptions().size();
            try {
                int idx = Integer.parseInt(v);
                if (idx >= 0 && idx < options) return Integer.toString(idx);
            } catch (NumberFormatException ignored) {
                // fall through
            }
            throw new BadRequestException("Choose one of the " + options + " options.");
        }
        if (!TITA.matcher(v).matches()) throw new BadRequestException("A TITA answer must be a number, such as 42, 3.5 or 2413.");
        return v;
    }
}

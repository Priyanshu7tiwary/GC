package com.catalyst.answerkey;

import com.catalyst.answerkey.dto.AdminKeyView;
import com.catalyst.answerkey.dto.AnswerKeyView;
import com.catalyst.answerkey.dto.CorrectAnswerRequest;
import com.catalyst.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class AnswerKeyController {
    private final AnswerKeyService keys;

    AnswerKeyController(AnswerKeyService keys) {
        this.keys = keys;
    }

    /** Keys admins have corrected in a previous-year paper. Answers are already revealed in review, so any signed-in user may read these. */
    @GetMapping("/api/mocks/{mockId}/answer-key-corrections")
    List<AnswerKeyView> corrections(@PathVariable String mockId) {
        return keys.corrections(mockId);
    }

    /** Admin only: the current key of every question in a previous-year paper (learners only see keys after submitting). */
    @GetMapping("/api/admin/mocks/{mockId}/answer-keys")
    List<AdminKeyView> keys(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String mockId) {
        return keys.keysFor(me.id(), mockId);
    }

    /** Admin only: replace a previous-year question's answer key. */
    @PutMapping("/api/admin/questions/{questionId}/answer")
    AnswerKeyView correct(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String questionId,
                          @Valid @RequestBody CorrectAnswerRequest req) {
        return keys.correct(me.id(), questionId, req.answer());
    }
}

package com.catalyst.revision;

import com.catalyst.revision.dto.ReviewRequest;
import com.catalyst.revision.dto.ReviewResult;
import com.catalyst.revision.dto.RevisionBoard;
import com.catalyst.revision.dto.RevisionEventView;
import com.catalyst.revision.dto.RevisionView;
import com.catalyst.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/revisions")
class RevisionController {
    private final RevisionService revisions;

    RevisionController(RevisionService revisions) {
        this.revisions = revisions;
    }

    /** Summary counts, 14-day forecast and every scheduled topic (paused topics included, flagged). */
    @GetMapping
    RevisionBoard board(@AuthenticationPrincipal AuthenticatedUser me) {
        return revisions.board(me.id());
    }

    /** Puts a topic in the schedule (first review tomorrow). Idempotent; resumes a paused topic without resetting it. */
    @PutMapping("/{topicId}")
    RevisionView enroll(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String topicId) {
        return revisions.enroll(me.id(), topicId);
    }

    record EnrollAll(@NotEmpty @Size(max = 200) List<@Size(max = 160) String> topicIds) {}

    /** Enrols several topics at once (topics marked learned elsewhere). Idempotent. */
    @PutMapping
    List<RevisionView> enrollAll(@AuthenticationPrincipal AuthenticatedUser me, @Valid @RequestBody EnrollAll req) {
        return revisions.enrollAll(me.id(), req.topicIds());
    }

    /** Records a Hard / Good / Easy review. Only a due review moves the schedule, at most once a day. */
    @PostMapping("/{topicId}/reviews")
    ReviewResult review(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String topicId, @Valid @RequestBody ReviewRequest req) {
        return revisions.review(me.id(), topicId, req.rating(), req.requestId());
    }

    /** Pauses a reopened topic: it stops coming due but keeps its stage and history. */
    @PostMapping("/{topicId}/pause")
    RevisionView pause(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String topicId) {
        return revisions.pause(me.id(), topicId);
    }

    /** Explicitly restarts a topic at the first rung, due tomorrow. History is kept. */
    @PostMapping("/{topicId}/reset")
    RevisionView reset(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String topicId) {
        return revisions.reset(me.id(), topicId);
    }

    /** Every enrolment, review, practice, pause, resume and reset for a topic, newest first. */
    @GetMapping("/{topicId}/history")
    List<RevisionEventView> history(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String topicId) {
        return revisions.history(me.id(), topicId);
    }
}

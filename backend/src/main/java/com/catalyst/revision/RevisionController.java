package com.catalyst.revision;

import com.catalyst.revision.dto.ReviewRequest;
import com.catalyst.revision.dto.RevisionBoard;
import com.catalyst.revision.dto.RevisionView;
import com.catalyst.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/revisions")
class RevisionController {
    private final RevisionService revisions;

    RevisionController(RevisionService revisions) {
        this.revisions = revisions;
    }

    /** Summary counts, 14-day forecast and every scheduled topic. */
    @GetMapping
    RevisionBoard board(@AuthenticationPrincipal AuthenticatedUser me) {
        return revisions.board(me.id());
    }

    /** Add a topic to the schedule (studied today). Idempotent. */
    @PutMapping("/{topicId}")
    @ResponseStatus(HttpStatus.CREATED)
    RevisionView enroll(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String topicId) {
        return revisions.enroll(me.id(), topicId);
    }

    /** Record a revision with a Hard / Good / Easy rating; returns the rescheduled item. */
    @PostMapping("/{topicId}/reviews")
    RevisionView review(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String topicId,
                        @Valid @RequestBody ReviewRequest req) {
        return revisions.review(me.id(), topicId, req.rating());
    }
}

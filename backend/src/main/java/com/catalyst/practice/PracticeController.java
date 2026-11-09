package com.catalyst.practice;

import com.catalyst.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/practice")
class PracticeController {
    private final PracticeService service;

    PracticeController(PracticeService service) {
        this.service = service;
    }

    /** For every topic: questions in its bank, keyed questions, and how many this learner has solved / marked. */
    @GetMapping("/topics")
    Map<String, PracticeService.TopicCounts> summary(@AuthenticationPrincipal AuthenticatedUser me) {
        return service.summary(me.id());
    }

    /** A topic's questions with this learner's state. filter: all | unsolved | solved | marked. */
    @GetMapping("/topics/{topicId}/questions")
    PracticeService.PracticePage topic(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String topicId,
                                       @RequestParam(defaultValue = "all") String filter,
                                       @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.topic(me.id(), topicId, filter, page, size);
    }

    record CheckRequest(@NotBlank @Size(max = 64) String answer) {}

    /** Checks an answer; a correct one marks the question solved. Returns the key and explanation. */
    @PostMapping("/questions/{questionId}/check")
    PracticeService.CheckResult check(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String questionId,
                                      @Valid @RequestBody CheckRequest req) {
        return service.check(me.id(), questionId, req.answer());
    }

    record MarkRequest(Boolean solved, Boolean marked) {}

    /** Marks a question solved / unsolved and/or for revision. */
    @PutMapping("/questions/{questionId}")
    PracticeService.PracticeQuestion mark(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String questionId,
                                          @RequestBody MarkRequest req) {
        return service.mark(me.id(), questionId, req.solved(), req.marked());
    }
}

package com.catalyst.question.admin;

import com.catalyst.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import com.catalyst.common.domain.Section;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin-only: grow the question bank that generated mocks draw from. */
@RestController
@Validated
@RequestMapping("/api/admin/questions")
class QuestionAdminController {
    private final QuestionAdminService service;

    QuestionAdminController(QuestionAdminService service) {
        this.service = service;
    }

    /** Keyed questions available to the generator, by section and origin. */
    @GetMapping("/stats")
    QuestionAdminService.PoolStats stats(@AuthenticationPrincipal AuthenticatedUser me) {
        return service.stats(me.id());
    }

    /** The question bank of every syllabus topic (and each section's unassigned questions), with counts by origin. */
    @GetMapping("/topics")
    List<QuestionAdminService.TopicBank> banks(@AuthenticationPrincipal AuthenticatedUser me) {
        return service.banks(me.id());
    }

    /** The questions in one topic's bank: ?topic=<topicId>, or ?topic=unassigned&section=QA. */
    @GetMapping
    QuestionAdminService.QuestionPage list(@AuthenticationPrincipal AuthenticatedUser me, @RequestParam String topic,
                                           @RequestParam(required = false) Section section,
                                           @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return service.questionsIn(me.id(), topic, section, page, size);
    }

    record MoveRequest(@NotBlank @Size(max = 160) String topicId) {}

    /** Moves a question to another topic's bank in the same section. */
    @PutMapping("/{questionId}/topic")
    QuestionAdminService.QuestionRow move(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String questionId,
                                          @Valid @RequestBody MoveRequest req) {
        return service.moveToTopic(me.id(), questionId, req.topicId());
    }

    /** Adds up to 500 questions in one request. All rows are validated before any is saved. */
    @PostMapping("/import")
    QuestionAdminService.ImportResult importQuestions(@AuthenticationPrincipal AuthenticatedUser me,
                                                      @RequestBody @NotEmpty @Size(max = 500) List<@Valid ImportQuestion> questions) {
        return service.importQuestions(me.id(), questions);
    }
}

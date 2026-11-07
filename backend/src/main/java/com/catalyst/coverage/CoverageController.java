package com.catalyst.coverage;

import com.catalyst.coverage.dto.CoverageView;
import com.catalyst.coverage.dto.UpdateCoverageRequest;
import com.catalyst.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coverage")
class CoverageController {
    private final CoverageService coverage;

    CoverageController(CoverageService coverage) {
        this.coverage = coverage;
    }

    @GetMapping
    CoverageView get(@AuthenticationPrincipal AuthenticatedUser me) {
        return coverage.view(me.id());
    }

    @PatchMapping("/{topicId}")
    CoverageView.TopicCoverage update(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String topicId,
                                      @Valid @RequestBody UpdateCoverageRequest req) {
        return coverage.update(me.id(), topicId, req);
    }
}

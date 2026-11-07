package com.catalyst.mock;

import com.catalyst.common.domain.Section;
import com.catalyst.mock.dto.MockPaper;
import com.catalyst.mock.dto.MockSummary;
import com.catalyst.mock.dto.YearSummary;
import com.catalyst.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mocks")
class MockController {
    private final MockCatalogService catalog;

    MockController(MockCatalogService catalog) {
        this.catalog = catalog;
    }

    /** e.g. ?kind=PREVIOUS_YEAR&year=2024, ?kind=SECTIONAL&section=QA, ?kind=CUSTOM */
    @GetMapping
    List<MockSummary> list(@AuthenticationPrincipal AuthenticatedUser me,
                           @RequestParam MockKind kind,
                           @RequestParam(required = false) Integer year,
                           @RequestParam(required = false) Section section) {
        return catalog.list(me.id(), kind, year, section);
    }

    @GetMapping("/years")
    List<YearSummary> years(@AuthenticationPrincipal AuthenticatedUser me) {
        return catalog.years(me.id());
    }

    @GetMapping("/{id}")
    MockSummary get(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String id) {
        return catalog.get(me.id(), id);
    }

    /** The question paper for the test player. Answer keys are never included. */
    @GetMapping("/{id}/paper")
    MockPaper paper(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String id) {
        return catalog.paper(me.id(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable String id) {
        catalog.deleteCustom(me.id(), id);
    }
}

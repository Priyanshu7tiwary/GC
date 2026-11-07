package com.catalyst.mock;

import com.catalyst.attempt.scoring.MarkingScheme;
import com.catalyst.common.domain.Section;
import com.catalyst.common.error.BadRequestException;
import com.catalyst.common.error.NotFoundException;
import com.catalyst.mock.dto.MockPaper;
import com.catalyst.mock.dto.MockSummary;
import com.catalyst.mock.dto.YearSummary;
import com.catalyst.question.QuestionType;
import com.catalyst.question.dto.QuestionView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MockCatalogService {
    private final MockRepository mocks;
    private final AttemptStatsProvider stats;
    private final QuestionSetResolver resolver;
    private final MarkingScheme marking;

    public MockCatalogService(MockRepository mocks, AttemptStatsProvider stats, QuestionSetResolver resolver, MarkingScheme marking) {
        this.mocks = mocks;
        this.stats = stats;
        this.resolver = resolver;
        this.marking = marking;
    }

    @Transactional(readOnly = true)
    public List<MockSummary> list(UUID userId, MockKind kind, Integer year, Section section) {
        List<Mock> found = kind == MockKind.CUSTOM
                ? mocks.findByKindAndOwnerIdOrderByCreatedAtDesc(kind, userId)
                : mocks.findByKindAndOwnerIdIsNullOrderByExamYearDescSlotAscIdAsc(kind);
        Map<String, AttemptStats> mine = stats.statsByMock(userId);
        return found.stream()
                .filter(m -> year == null || year.equals(m.getExamYear()))
                .filter(m -> section == null || m.getSections().contains(section))
                .map(m -> MockSummary.of(m, mine.get(m.getId())))
                .toList();
    }

    /** One entry per previous-year exam, newest first. */
    @Transactional(readOnly = true)
    public List<YearSummary> years(UUID userId) {
        Map<String, AttemptStats> mine = stats.statsByMock(userId);
        Map<Integer, List<Mock>> byYear = mocks.findByKindAndOwnerIdIsNullOrderByExamYearDescSlotAscIdAsc(MockKind.PREVIOUS_YEAR)
                .stream().collect(Collectors.groupingBy(Mock::getExamYear, LinkedHashMap::new, Collectors.toList()));
        List<YearSummary> out = new ArrayList<>();
        byYear.forEach((year, papers) -> out.add(new YearSummary(year, papers.size(),
                (int) papers.stream().filter(p -> mine.containsKey(p.getId())).count(),
                papers.getFirst().getQuestionCount(), papers.getFirst().getSectionMinutes())));
        return out;
    }

    @Transactional(readOnly = true)
    public Mock require(UUID userId, String mockId) {
        return mocks.findById(mockId).filter(m -> m.isVisibleTo(userId))
                .orElseThrow(() -> new NotFoundException("Mock not found: " + mockId));
    }

    @Transactional(readOnly = true)
    public MockSummary get(UUID userId, String mockId) {
        return MockSummary.of(require(userId, mockId), stats.statsByMock(userId).get(mockId));
    }

    @Transactional(readOnly = true)
    public MockPaper paper(UUID userId, String mockId) {
        Mock mock = require(userId, mockId);
        if (mock.getKind() == MockKind.PREVIOUS_YEAR
                && (mock.getQuestionIds() == null || mock.getQuestionIds().isEmpty())) {
            throw new BadRequestException("This previous-year paper is available as a source PDF, not as an interactive question set.");
        }
        var sections = resolver.resolve(mock).entrySet().stream()
                .map(e -> new MockPaper.PaperSection(e.getKey(), e.getValue().stream().map(QuestionView::of).toList()))
                .toList();
        var info = new MockPaper.MarkingInfo(marking.correct(QuestionType.MCQ), marking.wrong(QuestionType.MCQ), marking.wrong(QuestionType.TITA));
        return new MockPaper(mock.getId(), mock.getTitle(), mock.getSectionMinutes(), info, sections);
    }

    @Transactional
    public void deleteCustom(UUID userId, String mockId) {
        Mock mock = require(userId, mockId);
        if (mock.getKind() != MockKind.CUSTOM) throw new BadRequestException("Only custom mocks can be deleted.");
        mocks.delete(mock);
    }
}

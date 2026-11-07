package com.catalyst.generator;

import com.catalyst.common.domain.Section;
import com.catalyst.common.error.BadRequestException;
import com.catalyst.generator.dto.GenerationRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerationRequestTest {
    @Test
    void allThreeSubjectsBecomesAFullMock() {
        var req = new GenerationRequest(MockType.SECTIONAL, List.of(Section.QA, Section.VARC, Section.DILR),
                TargetDifficulty.HARD, true, true).normalized();
        assertThat(req.type()).isEqualTo(MockType.FULL);
        assertThat(req.sections()).containsExactly(Section.VARC, Section.DILR, Section.QA);
    }

    @Test
    void sectionalNeedsAtLeastOneSubject() {
        assertThatThrownBy(() -> new GenerationRequest(MockType.SECTIONAL, List.of(), TargetDifficulty.EASY, false, true).normalized())
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void duplicateSubjectsAreCollapsed() {
        var req = new GenerationRequest(MockType.SECTIONAL, List.of(Section.QA, Section.QA), TargetDifficulty.EASY, false, true).normalized();
        assertThat(req.sections()).containsExactly(Section.QA);
    }
}

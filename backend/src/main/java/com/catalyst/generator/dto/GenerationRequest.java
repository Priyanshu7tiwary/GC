package com.catalyst.generator.dto;

import com.catalyst.common.domain.Section;
import com.catalyst.common.error.BadRequestException;
import com.catalyst.generator.MockType;
import com.catalyst.generator.TargetDifficulty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Mirrors the generator form: pick FULL or SECTIONAL; a sectional needs subjects;
 * selecting all three subjects is the same as a full mock.
 */
public record GenerationRequest(@NotNull MockType type, @jakarta.validation.constraints.Size(max = 3) List<Section> sections, @NotNull TargetDifficulty difficulty,
                                boolean focusWeakTopics, boolean strictTiming) {

    public GenerationRequest normalized() {
        List<Section> chosen = sections == null ? List.of() : sections.stream().distinct().sorted().toList();
        if (type == MockType.FULL || chosen.size() == Section.values().length) {
            return new GenerationRequest(MockType.FULL, List.of(Section.values()), difficulty, focusWeakTopics, strictTiming);
        }
        if (chosen.isEmpty()) throw new BadRequestException("Choose at least one subject for a sectional mock.");
        return new GenerationRequest(MockType.SECTIONAL, chosen, difficulty, focusWeakTopics, strictTiming);
    }
}

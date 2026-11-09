package com.catalyst.generator.dto;

import com.catalyst.common.domain.Section;
import com.catalyst.common.error.BadRequestException;
import com.catalyst.generator.MockType;
import com.catalyst.generator.TargetDifficulty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Mirrors the generator form: pick FULL or SECTIONAL; a sectional needs subjects;
 * selecting all three subjects is the same as a full mock.
 * focusTopics: syllabus topic names the client considers weak (its coverage lives in the browser); when empty,
 * the server's own coverage records are used.
 */
public record GenerationRequest(@NotNull MockType type, @Size(max = 3) List<Section> sections, @NotNull TargetDifficulty difficulty,
                                boolean focusWeakTopics, boolean strictTiming,
                                @Size(max = 12) List<@Size(max = 120) String> focusTopics) {

    public GenerationRequest(MockType type, List<Section> sections, TargetDifficulty difficulty, boolean focusWeakTopics, boolean strictTiming) {
        this(type, sections, difficulty, focusWeakTopics, strictTiming, List.of());
    }

    public GenerationRequest normalized() {
        List<Section> chosen = sections == null ? List.of() : sections.stream().distinct().sorted().toList();
        List<String> topics = focusTopics == null ? List.of() : focusTopics.stream().filter(t -> t != null && !t.isBlank()).map(String::trim).distinct().toList();
        if (type == MockType.FULL || chosen.size() == Section.values().length) {
            return new GenerationRequest(MockType.FULL, List.of(Section.values()), difficulty, focusWeakTopics, strictTiming, topics);
        }
        if (chosen.isEmpty()) throw new BadRequestException("Choose at least one subject for a sectional mock.");
        return new GenerationRequest(MockType.SECTIONAL, chosen, difficulty, focusWeakTopics, strictTiming, topics);
    }
}

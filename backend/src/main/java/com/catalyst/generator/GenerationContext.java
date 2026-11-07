package com.catalyst.generator;

import com.catalyst.common.domain.Section;
import com.catalyst.generator.dto.GenerationRequest;
import com.catalyst.question.Question;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Mutable state handed from one pipeline step to the next. */
public class GenerationContext {
    private final UUID userId;
    private final GenerationRequest request;
    private List<String> weakTopics = List.of();
    private final Map<Section, Integer> blueprint = new LinkedHashMap<>();
    private final Map<Section, List<Question>> questions = new LinkedHashMap<>();
    private String resultMockId;

    public GenerationContext(UUID userId, GenerationRequest request) {
        this.userId = userId;
        this.request = request;
    }

    public UUID userId() { return userId; }
    public GenerationRequest request() { return request; }
    public List<Section> sections() { return request.sections(); }
    public List<String> weakTopics() { return weakTopics; }
    public void weakTopics(List<String> names) { this.weakTopics = List.copyOf(names); }
    public Map<Section, Integer> blueprint() { return blueprint; }
    public Map<Section, List<Question>> questions() { return questions; }
    public String resultMockId() { return resultMockId; }
    public void resultMockId(String id) { this.resultMockId = id; }
}

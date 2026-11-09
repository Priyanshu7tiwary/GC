package com.catalyst.generator;

import com.catalyst.common.domain.Section;
import com.catalyst.generator.ai.DraftQuestion;
import com.catalyst.generator.dto.GenerationRequest;
import com.catalyst.generator.plan.CoveragePicker;
import com.catalyst.generator.plan.SectionPlan;
import com.catalyst.question.Question;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Mutable state handed from one pipeline step to the next. */
public class GenerationContext {
    private final UUID userId;
    private final GenerationRequest request;
    private List<String> weakTopics = List.of();
    private Set<String> recentlyUsed = Set.of();
    private final Map<Section, Integer> blueprint = new LinkedHashMap<>();
    private final Map<Section, List<Question>> questions = new LinkedHashMap<>();
    private final Map<Section, List<DraftQuestion>> drafts = new LinkedHashMap<>();
    private final Map<Section, SectionPlan> plans = new LinkedHashMap<>();
    private final Map<Section, CoveragePicker.Selection> coverage = new LinkedHashMap<>();
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
    /** Questions this learner already had in earlier generated mocks; the picker avoids them while it can. */
    public Set<String> recentlyUsed() { return recentlyUsed; }
    public void recentlyUsed(Set<String> ids) { this.recentlyUsed = Set.copyOf(ids); }
    public Map<Section, Integer> blueprint() { return blueprint; }
    public Map<Section, List<Question>> questions() { return questions; }
    /** New questions Claude drafted for this mock; the validation step keeps only those an independent solve agrees with. */
    public Map<Section, List<DraftQuestion>> drafts() { return drafts; }
    /** The blueprint: what each section should cover. */
    public Map<Section, SectionPlan> plans() { return plans; }
    /** What the picked questions actually cover, per section. */
    public Map<Section, CoveragePicker.Selection> coverage() { return coverage; }
    public String resultMockId() { return resultMockId; }
    public void resultMockId(String id) { this.resultMockId = id; }
}

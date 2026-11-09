package com.catalyst.generator.steps;

import com.catalyst.coverage.WeakTopicFinder;
import com.catalyst.coverage.dto.CoverageView.TopicCoverage;
import com.catalyst.generator.GenerationContext;
import com.catalyst.generator.GenerationStep;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(10)
class ProfileAnalysisStep implements GenerationStep {
    private final WeakTopicFinder weakTopics;

    ProfileAnalysisStep(WeakTopicFinder weakTopics) {
        this.weakTopics = weakTopics;
    }

    public String id() { return "profile"; }
    public String label() { return "Analysing your performance"; }
    public String detail() { return "Reading past attempts & coverage to find weak areas"; }

    @Override
    public String execute(GenerationContext ctx) {
        if (!ctx.request().focusWeakTopics()) return "Weak-topic focus off — balanced mix";
        List<String> fromClient = ctx.request().focusTopics();
        ctx.weakTopics(fromClient != null && !fromClient.isEmpty() ? fromClient
                : weakTopics.find(ctx.userId(), ctx.sections(), 6).stream().map(TopicCoverage::name).toList());
        return ctx.weakTopics().size() + " weak topics found";
    }
}

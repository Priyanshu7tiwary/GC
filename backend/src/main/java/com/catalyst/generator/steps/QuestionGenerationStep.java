package com.catalyst.generator.steps;

import com.catalyst.generator.GenerationContext;
import com.catalyst.generator.GenerationStep;
import com.catalyst.generator.source.QuestionSource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
class QuestionGenerationStep implements GenerationStep {
    private final QuestionSource source;

    QuestionGenerationStep(QuestionSource source) {
        this.source = source;
    }

    public String id() { return "generate"; }
    public String label() { return "Generating questions"; }
    public String detail() { return "Drafting passages, sets and questions"; }

    @Override
    public String execute(GenerationContext ctx) {
        ctx.blueprint().forEach((section, count) ->
                ctx.questions().put(section, source.fetch(section, count, ctx.request().difficulty(), ctx.weakTopics())));
        int got = ctx.questions().values().stream().mapToInt(java.util.List::size).sum();
        int wanted = ctx.blueprint().values().stream().mapToInt(Integer::intValue).sum();
        return got + " of " + wanted + " questions prepared";
    }
}

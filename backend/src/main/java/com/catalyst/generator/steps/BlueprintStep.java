package com.catalyst.generator.steps;

import com.catalyst.common.domain.Section;
import com.catalyst.generator.GenerationContext;
import com.catalyst.generator.GenerationStep;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@Order(20)
class BlueprintStep implements GenerationStep {
    public String id() { return "blueprint"; }
    public String label() { return "Building the test blueprint"; }
    public String detail() { return "Choosing topic mix and difficulty curve per section"; }

    @Override
    public String execute(GenerationContext ctx) {
        for (Section s : ctx.sections()) ctx.blueprint().put(s, s.standardQuestions());
        return ctx.blueprint().entrySet().stream().map(e -> e.getKey() + " " + e.getValue()).collect(Collectors.joining(" · "))
                + " · " + ctx.request().difficulty().label();
    }
}

package com.catalyst.generator.steps;

import com.catalyst.common.domain.Section;
import com.catalyst.generator.GenerationContext;
import com.catalyst.generator.GenerationStep;
import com.catalyst.generator.MockType;
import com.catalyst.mock.Mock;
import com.catalyst.mock.MockRepository;
import com.catalyst.question.Question;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Order(50)
class AssemblyStep implements GenerationStep {
    private final MockRepository mocks;
    private final Clock clock;

    AssemblyStep(MockRepository mocks, Clock clock) {
        this.mocks = mocks;
        this.clock = clock;
    }

    public String id() { return "assemble"; }
    public String label() { return "Assembling your mock"; }
    public String detail() { return "Ordering sections, timing and marking scheme"; }

    @Override
    public String execute(GenerationContext ctx) {
        var req = ctx.request();
        List<String> ids = ctx.questions().values().stream().flatMap(List::stream).map(Question::getId).toList();
        String title = req.type() == MockType.FULL ? "AI Full Mock"
                : "AI " + ctx.sections().stream().map(Section::name).collect(Collectors.joining(" + ")) + " Sectional";
        long topicsCovered = ctx.questions().values().stream().flatMap(List::stream).map(Question::getTopicId)
                .filter(java.util.Objects::nonNull).distinct().count();
        String subtitle = req.difficulty().label() + (req.focusWeakTopics() ? " · weak-area focus" : "") + " · " + topicsCovered + " topics";
        int minutes = req.strictTiming() ? 40 : 60;
        int questionCount = ids.size();

        Mock mock = Mock.custom("custom-" + java.util.UUID.randomUUID().toString().substring(0, 13), ctx.userId(), title, subtitle,
                ctx.sections(), minutes, questionCount, req.difficulty().label(), ids, ctx.weakTopics(), clock.instant());
        mocks.save(mock);
        ctx.resultMockId(mock.getId());
        return title + " ready · " + minutes * ctx.sections().size() + " min";
    }
}

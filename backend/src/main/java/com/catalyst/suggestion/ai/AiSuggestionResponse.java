package com.catalyst.suggestion.ai;

import java.time.Instant;
import java.util.List;

/** The AI's study plan. Every topic and test is one that exists, and every link is built by the server. */
public record AiSuggestionResponse(String summary, Likelihood likelihood, List<TopicPick> topics, List<TestPick> tests, String model, Instant generatedAt) {
    /**
     * How likely the learner is to reach their target percentile, 1-99. label is set by the server from percent, and
     * confidence is forced to "low" when there are no full-length mocks to judge from.
     */
    public record Likelihood(int percent, String label, String confidence, String reason, Double realisticPercentile) {}

    public record TopicPick(String topicId, String name, String section, String chapter, String status, int questionsInBank, int solved,
                            String priority, String reason, String link) {}

    public record TestPick(String mockId, String title, String kind, boolean attempted, String reason, String link) {}
}

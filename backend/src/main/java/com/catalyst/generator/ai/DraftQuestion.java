package com.catalyst.generator.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** A question Claude drafted. It is not trusted until an independent solve agrees with {@link #answer()}. */
public record DraftQuestion(
        @JsonPropertyDescription("MCQ or TITA") String type,
        @JsonPropertyDescription("The syllabus topic, using one of the topic names given in the instructions") String topic,
        @JsonPropertyDescription("The complete, self-contained question. Write maths as TeX between \\( and \\).") String prompt,
        @JsonPropertyDescription("Exactly four distinct options for MCQ; an empty list for TITA") List<String> options,
        @JsonPropertyDescription("MCQ: the zero-based index of the correct option (\"0\" to \"3\"). TITA: the exact numeric answer, digits only") String answer,
        @JsonPropertyDescription("A concise worked solution") String explanation,
        @JsonPropertyDescription("EASY, MODERATE or HARD") String difficulty) {

    public record Batch(List<DraftQuestion> questions) {}
}

package com.catalyst.answerkey.dto;

/** The current key for a question. MCQ answers are zero-based option indices. */
public record AnswerKeyView(String questionId, String answer) {}

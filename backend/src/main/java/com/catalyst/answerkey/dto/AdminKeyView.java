package com.catalyst.answerkey.dto;

/** A question's current key for admins. answer is null when the question has no key and is not scored. */
public record AdminKeyView(String questionId, String answer, boolean corrected) {}

package com.catalyst.common.domain;

/** The three CAT sections, with the maximum marks each carries in the current paper pattern. */
public enum Section {
    VARC(24), DILR(22), QA(22);

    private final int standardQuestions;

    Section(int standardQuestions) {
        this.standardQuestions = standardQuestions;
    }

    public int standardQuestions() {
        return standardQuestions;
    }

    public int standardMaxMarks() {
        return standardQuestions * 3;
    }
}

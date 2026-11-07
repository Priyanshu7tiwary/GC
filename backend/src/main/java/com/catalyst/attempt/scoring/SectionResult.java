package com.catalyst.attempt.scoring;

public record SectionResult(int correct, int wrong, int unattempted, int score, int maxScore, int timeSeconds) {
    public int attempted() { return correct + wrong; }

    public double accuracy() { return attempted() == 0 ? 0 : (double) correct / attempted(); }
}

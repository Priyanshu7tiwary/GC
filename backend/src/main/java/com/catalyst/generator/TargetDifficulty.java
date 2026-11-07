package com.catalyst.generator;

import com.catalyst.question.Difficulty;

import java.util.Optional;

public enum TargetDifficulty {
    EASY("Easy"), MODERATE("Moderate"), HARD("Hard"), CAT_LEVEL("CAT-level"), ADAPTIVE("Adaptive");

    private final String label;

    TargetDifficulty(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** A single bank difficulty to filter on, or empty for mixed levels. */
    public Optional<Difficulty> fixedLevel() {
        return switch (this) {
            case EASY -> Optional.of(Difficulty.EASY);
            case MODERATE -> Optional.of(Difficulty.MODERATE);
            case HARD -> Optional.of(Difficulty.HARD);
            case CAT_LEVEL, ADAPTIVE -> Optional.empty();
        };
    }
}

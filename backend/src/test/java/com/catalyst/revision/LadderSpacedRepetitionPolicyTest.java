package com.catalyst.revision;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LadderSpacedRepetitionPolicyTest {
    private final LadderSpacedRepetitionPolicy policy = new LadderSpacedRepetitionPolicy();

    @Test
    void hardResetsGoodClimbsOneEasyClimbsTwo() {
        assertThat(policy.nextStage(3, Rating.HARD)).isZero();
        assertThat(policy.nextStage(1, Rating.GOOD)).isEqualTo(2);
        assertThat(policy.nextStage(1, Rating.EASY)).isEqualTo(3);
    }

    @Test
    void capsAtTheTopRung() {
        assertThat(policy.nextStage(5, Rating.EASY)).isEqualTo(5);
        assertThat(policy.intervalDays(5)).isEqualTo(60);
    }

    @Test
    void intervalsFollowTheLadder() {
        assertThat(new int[]{policy.intervalDays(0), policy.intervalDays(1), policy.intervalDays(2), policy.intervalDays(3)})
                .containsExactly(1, 3, 7, 15);
    }
}

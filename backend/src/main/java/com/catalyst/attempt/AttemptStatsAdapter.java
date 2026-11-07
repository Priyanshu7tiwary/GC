package com.catalyst.attempt;

import com.catalyst.mock.AttemptStats;
import com.catalyst.mock.AttemptStatsProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Supplies the mock catalogue with per-learner attempt stats (implements the catalogue's port). */
@Component
class AttemptStatsAdapter implements AttemptStatsProvider {
    private final AttemptRepository attempts;

    AttemptStatsAdapter(AttemptRepository attempts) {
        this.attempts = attempts;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, AttemptStats> statsByMock(UUID userId) {
        return attempts.findByUserIdOrderBySubmittedAtAsc(userId).stream()
                .collect(Collectors.groupingBy(Attempt::getMockId)).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    List<Attempt> list = e.getValue();
                    Attempt last = list.getLast();
                    return new AttemptStats(list.size(), list.stream().mapToInt(Attempt::getScore).max().orElse(0),
                            last.getId(), last.getPercentile());
                }));
    }
}

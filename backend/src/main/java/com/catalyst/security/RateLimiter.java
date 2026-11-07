package com.catalyst.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window limiter kept in memory. Fine for a single API instance;
 * swap for a Redis-backed implementation when running several replicas.
 */
@Component
public class RateLimiter {
    private static final int MAX_KEYS = 50_000;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Records a hit and returns false when the key has exceeded {@code limit} hits within {@code window}. */
    public boolean tryAcquire(String key, int limit, Duration window) {
        long now = clock.millis(), cutoff = now - window.toMillis();
        if (hits.size() > MAX_KEYS) hits.clear(); // crude memory bound against key-flooding
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst() < cutoff) q.pollFirst();
            if (q.size() >= limit) return false;
            q.addLast(now);
            return true;
        }
    }
}

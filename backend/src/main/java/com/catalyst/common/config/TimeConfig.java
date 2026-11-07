package com.catalyst.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** A single injectable clock so date logic (streaks, due dates) is testable and uses IST like the exam. */
@Configuration
public class TimeConfig {
    public static final ZoneId EXAM_ZONE = ZoneId.of("Asia/Kolkata");

    @Bean
    Clock clock() {
        return Clock.system(EXAM_ZONE);
    }
}

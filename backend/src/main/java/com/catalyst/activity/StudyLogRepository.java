package com.catalyst.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface StudyLogRepository extends JpaRepository<StudyLog, Long> {
    List<StudyLog> findByUserIdAndDayGreaterThanEqualOrderByDayAsc(UUID userId, LocalDate from);

    /** Atomically adds minutes to a day, creating the row on first use. */
    @Modifying
    @Query(value = """
            INSERT INTO study_logs (user_id, day, minutes) VALUES (:userId, :day, :minutes)
            ON CONFLICT (user_id, day) DO UPDATE SET minutes = study_logs.minutes + EXCLUDED.minutes
            """, nativeQuery = true)
    void addMinutes(@Param("userId") UUID userId, @Param("day") LocalDate day, @Param("minutes") int minutes);
}

package com.catalyst.revision;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Daily snapshots of a learner's revision backlog (plain SQL: a two-column key needs no entity class). */
public interface BacklogDayRepository extends Repository<RevisionItem, Long> {
    @Modifying
    @Query(value = """
            INSERT INTO revision_backlog_days (user_id, day, overdue, due) VALUES (:user, :day, :overdue, :due)
            ON CONFLICT (user_id, day) DO UPDATE SET overdue = EXCLUDED.overdue, due = EXCLUDED.due""", nativeQuery = true)
    void upsert(@Param("user") UUID userId, @Param("day") LocalDate day, @Param("overdue") int overdue, @Param("due") int due);

    /** [day, overdue, due] from a date on, oldest first. */
    @Query(value = "SELECT day, overdue, due FROM revision_backlog_days WHERE user_id = :user AND day >= :from ORDER BY day", nativeQuery = true)
    List<Object[]> since(@Param("user") UUID userId, @Param("from") LocalDate from);
}

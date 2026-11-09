-- Revision scheduling rules: only due reviews advance, at most once per calendar day; reopening a topic pauses its
-- schedule instead of deleting it; every enrolment, review, practice, pause, resume and reset is recorded.
ALTER TABLE revision_items ADD COLUMN enrolled_on DATE;
ALTER TABLE revision_items ADD COLUMN last_advanced_on DATE;
ALTER TABLE revision_items ADD COLUMN paused BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE revision_items SET enrolled_on = last_revised_on,
                          last_advanced_on = CASE WHEN review_count > 1 THEN last_revised_on END;
ALTER TABLE revision_items ALTER COLUMN enrolled_on SET NOT NULL;

CREATE TABLE revision_events (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    topic_id    VARCHAR(160) NOT NULL REFERENCES topics (id),
    kind        VARCHAR(16)  NOT NULL,   -- ENROLL, REVIEW, PRACTICE, PAUSE, RESUME, RESET
    rating      VARCHAR(8),              -- HARD, GOOD, EASY for REVIEW and PRACTICE
    occurred_on DATE         NOT NULL,   -- the learner's calendar date (exam time zone)
    prior_stage INTEGER,
    new_stage   INTEGER,
    prior_due   DATE,
    new_due     DATE,
    request_id  VARCHAR(64),             -- client-generated; a retried request is recognised and not applied twice
    created_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_revision_events_topic ON revision_events (user_id, topic_id, created_at);
CREATE UNIQUE INDEX uq_revision_event_request ON revision_events (user_id, request_id) WHERE request_id IS NOT NULL;

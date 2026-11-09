-- Each AI study plan a learner generates. Counting them gives the weekly limit; the latest one is shown again
-- (with the countdown to the next) once the limit is used up.
CREATE TABLE ai_suggestion_runs (
    id         BIGSERIAL PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    response   TEXT        NOT NULL
);
CREATE INDEX ix_ai_suggestion_runs_user ON ai_suggestion_runs (user_id, created_at);

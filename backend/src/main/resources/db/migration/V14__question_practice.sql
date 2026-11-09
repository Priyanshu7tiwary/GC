-- Per-question practice state: which questions of a topic's bank the learner has solved or marked for revision.
CREATE TABLE question_practice (
    id           BIGSERIAL PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    question_id  VARCHAR(40) NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    solved       BOOLEAN     NOT NULL DEFAULT FALSE,
    marked       BOOLEAN     NOT NULL DEFAULT FALSE,   -- marked for revision
    attempts     INTEGER     NOT NULL DEFAULT 0,       -- answers checked
    last_answer  VARCHAR(64),
    last_correct BOOLEAN,
    solved_at    TIMESTAMPTZ,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_question_practice UNIQUE (user_id, question_id)
);
CREATE INDEX ix_question_practice_user ON question_practice (user_id) WHERE solved OR marked;

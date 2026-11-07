-- Core schema for CATalyst

CREATE TABLE users (
    id                UUID PRIMARY KEY,
    email             VARCHAR(255) NOT NULL UNIQUE,
    name              VARCHAR(120) NOT NULL,
    password_hash     VARCHAR(100) NOT NULL,
    target_percentile DOUBLE PRECISION NOT NULL,
    target_year       INTEGER NOT NULL,
    exam_date         DATE NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE topics (
    id         VARCHAR(160) PRIMARY KEY,
    section    VARCHAR(8)   NOT NULL,
    chapter    VARCHAR(80)  NOT NULL,
    name       VARCHAR(120) NOT NULL,
    high_yield BOOLEAN      NOT NULL DEFAULT FALSE,
    position   INTEGER      NOT NULL
);

CREATE TABLE topic_progress (
    id         BIGSERIAL PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    topic_id   VARCHAR(160) NOT NULL REFERENCES topics (id),
    status     VARCHAR(16)  NOT NULL,
    practiced  INTEGER      NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_topic_progress UNIQUE (user_id, topic_id)
);

CREATE TABLE revision_items (
    id              BIGSERIAL PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    topic_id        VARCHAR(160) NOT NULL REFERENCES topics (id),
    stage           INTEGER      NOT NULL,
    last_revised_on DATE         NOT NULL,
    next_due_on     DATE         NOT NULL,
    review_count    INTEGER      NOT NULL,
    CONSTRAINT uq_revision_item UNIQUE (user_id, topic_id)
);
CREATE INDEX ix_revision_due ON revision_items (user_id, next_due_on);

CREATE TABLE study_logs (
    id      BIGSERIAL PRIMARY KEY,
    user_id UUID    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    day     DATE    NOT NULL,
    minutes INTEGER NOT NULL,
    CONSTRAINT uq_study_log UNIQUE (user_id, day)
);

CREATE TABLE questions (
    id          VARCHAR(40)  PRIMARY KEY,
    section     VARCHAR(8)   NOT NULL,
    topic       VARCHAR(120) NOT NULL,
    type        VARCHAR(8)   NOT NULL,
    prompt      TEXT         NOT NULL,
    passage     TEXT,
    options     JSONB,
    answer      VARCHAR(64)  NOT NULL,
    explanation TEXT         NOT NULL,
    difficulty  VARCHAR(16)  NOT NULL,
    position    INTEGER      NOT NULL
);
CREATE INDEX ix_questions_section ON questions (section);

CREATE TABLE mocks (
    id                      VARCHAR(64)  PRIMARY KEY,
    kind                    VARCHAR(16)  NOT NULL,
    title                   VARCHAR(120) NOT NULL,
    subtitle                VARCHAR(160),
    exam_year               INTEGER,
    slot                    INTEGER,
    sections                JSONB        NOT NULL,
    section_question_counts JSONB,
    section_minutes         INTEGER      NOT NULL,
    question_count          INTEGER      NOT NULL,
    difficulty              VARCHAR(32)  NOT NULL,
    owner_id                UUID REFERENCES users (id) ON DELETE CASCADE,
    question_ids            JSONB,
    focus_topics            JSONB,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_mocks_kind ON mocks (kind, exam_year);
CREATE INDEX ix_mocks_owner ON mocks (owner_id);

-- Attempts keep a copy of the mock title so history survives a custom mock being deleted
CREATE TABLE attempts (
    id                 UUID PRIMARY KEY,
    user_id            UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    mock_id            VARCHAR(64)  NOT NULL,
    mock_title         VARCHAR(120) NOT NULL,
    full_length        BOOLEAN      NOT NULL,
    submitted_at       TIMESTAMPTZ  NOT NULL,
    score              INTEGER      NOT NULL,
    max_score          INTEGER      NOT NULL,
    percentile         DOUBLE PRECISION,
    time_spent_seconds INTEGER      NOT NULL,
    section_results    JSONB        NOT NULL,
    answers            JSONB        NOT NULL,
    question_ids       JSONB        NOT NULL
);
CREATE INDEX ix_attempts_user ON attempts (user_id, submitted_at);

CREATE TABLE generation_jobs (
    id             UUID PRIMARY KEY,
    user_id        UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status         VARCHAR(16) NOT NULL,
    request        JSONB       NOT NULL,
    steps          JSONB       NOT NULL,
    result_mock_id VARCHAR(64),
    error          TEXT,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

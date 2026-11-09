-- Admins may correct previous-year answer keys. Grant with: UPDATE users SET is_admin = TRUE WHERE email = '...';
-- (or list verified emails in the ADMIN_EMAILS environment variable).
ALTER TABLE users ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT FALSE;

-- Every change to a key is kept, newest last; questions.answer always holds the current key.
CREATE TABLE answer_key_corrections (
    id              UUID PRIMARY KEY,
    question_id     VARCHAR(40)  NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    previous_answer VARCHAR(64),
    new_answer      VARCHAR(64)  NOT NULL,
    corrected_by    UUID REFERENCES users (id) ON DELETE SET NULL,
    corrected_at    TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_answer_key_corrections_question ON answer_key_corrections (question_id, corrected_at);

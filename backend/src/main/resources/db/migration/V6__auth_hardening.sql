-- Google sign-in and server-side session revocation.

-- Google's stable account id ("sub" claim). An account may have a password, a Google link, or both.
ALTER TABLE users ADD COLUMN google_subject VARCHAR(255);
ALTER TABLE users ADD CONSTRAINT uq_users_google_subject UNIQUE (google_subject);
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- Bumped on logout; tokens carrying an older version are rejected, so logging out ends every session.
ALTER TABLE users ADD COLUMN session_version INTEGER NOT NULL DEFAULT 0;

-- Emails are compared case-insensitively everywhere; enforce it in the database too.
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));

-- Email ownership is proven by a one-time code (or by Google).
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE users SET email_verified = TRUE WHERE google_subject IS NOT NULL;

-- One-time codes for sign-up verification and two-step password sign-in. Only a keyed hash of the code is stored.
CREATE TABLE otp_challenges (
    id           UUID PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose      VARCHAR(16) NOT NULL,
    code_hash    VARCHAR(64) NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    attempts     INTEGER     NOT NULL DEFAULT 0,
    sends        INTEGER     NOT NULL DEFAULT 1,
    last_sent_at TIMESTAMPTZ NOT NULL,
    consumed_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_otp_user_created ON otp_challenges (user_id, created_at);

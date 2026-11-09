-- One row per learner per day: how many revision topics were overdue and due, so Analytics can show the trend.
CREATE TABLE revision_backlog_days (
    user_id UUID    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    day     DATE    NOT NULL,
    overdue INTEGER NOT NULL,
    due     INTEGER NOT NULL,
    PRIMARY KEY (user_id, day)
);

-- Whether a plan counts towards its learner's weekly limit. Resetting the limit sets this to false and keeps the plan.
ALTER TABLE ai_suggestion_runs ADD COLUMN counted BOOLEAN NOT NULL DEFAULT TRUE;

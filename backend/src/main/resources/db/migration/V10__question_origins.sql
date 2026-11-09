-- Where a question came from: the original practice bank, an official CAT paper, an admin import, or drafted by Claude
-- during mock generation (kept only after an independent solve agreed with its key).
ALTER TABLE questions ADD COLUMN origin VARCHAR(16) NOT NULL DEFAULT 'BANK';
ALTER TABLE questions ADD COLUMN created_at TIMESTAMPTZ;
UPDATE questions SET origin = 'PYQ' WHERE paper_id IS NOT NULL;
CREATE INDEX ix_questions_pool ON questions (section, origin) WHERE answer IS NOT NULL;

-- Soft-delete support for resumes (EDS-31).
-- Resumes are never physically removed via the API; they are flagged with a
-- deletion timestamp and excluded from all owner-scoped queries.
ALTER TABLE resumes ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

-- Partial index keeps the common "active resumes for a user" lookups fast while
-- ignoring soft-deleted rows.
CREATE INDEX IF NOT EXISTS idx_resumes_user_active
    ON resumes (user_id)
    WHERE deleted_at IS NULL;

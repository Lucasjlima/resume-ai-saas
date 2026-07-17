CREATE TABLE IF NOT EXISTS resume_regenerations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_id BIGINT NOT NULL,
    generated_json JSONB,
    pdf_storage_path TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    attempt_number INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_resume_regenerations_analysis FOREIGN KEY (analysis_id) REFERENCES analyses(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_resume_regenerations_analysis_id ON resume_regenerations(analysis_id);

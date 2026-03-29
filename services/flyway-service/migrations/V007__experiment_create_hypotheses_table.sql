-- Service: experiment-service
-- Schema: experiment_schema
-- Description: Create hypotheses table

-- Set search path to target schema
SET search_path TO experiment_schema;

-- Create hypotheses table
CREATE TABLE IF NOT EXISTS hypotheses (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    problem_id VARCHAR(36) NOT NULL,
    statement VARCHAR(500) NOT NULL,
    expected_outcome TEXT,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Add foreign key constraint if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_hypotheses_problem_id'
    ) THEN
        ALTER TABLE hypotheses 
        ADD CONSTRAINT fk_hypotheses_problem_id 
        FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_hypotheses_org_id ON hypotheses(organization_id);
CREATE INDEX IF NOT EXISTS idx_hypotheses_problem_id ON hypotheses(problem_id);
CREATE INDEX IF NOT EXISTS idx_hypotheses_created_at ON hypotheses(created_at);

-- Reset search path
SET search_path TO public;

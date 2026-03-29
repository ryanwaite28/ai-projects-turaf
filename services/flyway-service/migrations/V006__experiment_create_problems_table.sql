-- Service: experiment-service
-- Schema: experiment_schema
-- Description: Create problems table

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS experiment_schema;

-- Set search path to target schema
SET search_path TO experiment_schema;

-- Create problems table
CREATE TABLE IF NOT EXISTS problems (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_problems_org_id ON problems(organization_id);
CREATE INDEX IF NOT EXISTS idx_problems_created_at ON problems(created_at);

-- Reset search path
SET search_path TO public;

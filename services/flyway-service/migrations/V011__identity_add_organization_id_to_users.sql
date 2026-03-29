-- Service: identity-service
-- Schema: identity_schema
-- Description: Add organization_id column to users table

-- Set search path to target schema
SET search_path TO identity_schema;

-- Add organization_id column
ALTER TABLE users ADD COLUMN IF NOT EXISTS organization_id VARCHAR(36);

-- Backfill existing rows with a placeholder (if any exist)
UPDATE users SET organization_id = '00000000-0000-0000-0000-000000000000' WHERE organization_id IS NULL;

-- Make column NOT NULL after backfill
ALTER TABLE users ALTER COLUMN organization_id SET NOT NULL;

-- Create index for organization lookups
CREATE INDEX IF NOT EXISTS idx_users_organization_id ON users(organization_id);

-- Reset search path
SET search_path TO public;

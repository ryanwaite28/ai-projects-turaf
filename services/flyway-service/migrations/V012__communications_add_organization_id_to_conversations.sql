-- Service: communications-service
-- Schema: communications_schema
-- Description: Add organization_id column to conversations table

-- Set search path to target schema
SET search_path TO communications_schema;

-- Add organization_id column
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255);

-- Backfill existing rows with a placeholder (if any exist)
UPDATE conversations SET organization_id = '00000000-0000-0000-0000-000000000000' WHERE organization_id IS NULL;

-- Make column NOT NULL after backfill
ALTER TABLE conversations ALTER COLUMN organization_id SET NOT NULL;

-- Create index for organization lookups
CREATE INDEX IF NOT EXISTS idx_conversations_organization_id ON conversations(organization_id);

-- Reset search path
SET search_path TO public;

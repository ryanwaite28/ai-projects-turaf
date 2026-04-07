-- V016: Fix organization_id column type in conversations table
-- Issue: V012 created organization_id as VARCHAR(255), should be VARCHAR(36) for UUID consistency
-- Service: Communications Service
-- Schema: communications_schema

SET search_path TO communications_schema;

-- Alter organization_id column type from VARCHAR(255) to VARCHAR(36)
ALTER TABLE conversations 
ALTER COLUMN organization_id TYPE VARCHAR(36);

-- Add comment for documentation
COMMENT ON COLUMN conversations.organization_id IS 'Organization ID (UUID format, VARCHAR(36))';

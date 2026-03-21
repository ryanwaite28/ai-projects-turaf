-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS organization_schema;

-- Create organizations table
CREATE TABLE IF NOT EXISTS organizations (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(50) NOT NULL UNIQUE,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    allow_public_experiments BOOLEAN NOT NULL DEFAULT FALSE,
    max_members INT NOT NULL DEFAULT 10,
    max_experiments INT NOT NULL DEFAULT 100
);

-- Create index on slug for fast lookups
CREATE INDEX IF NOT EXISTS idx_organizations_slug ON organizations(slug);

-- Create index on created_by for user's organizations lookup
CREATE INDEX IF NOT EXISTS idx_organizations_created_by ON organizations(created_by);

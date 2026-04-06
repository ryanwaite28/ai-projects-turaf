-- Service: identity-service
-- Schema: identity_schema
-- Description: Create user_roles table for role-based access control
-- Related: D7 (DB Migration vs Spec/Entity Discrepancies)

-- Set search path to target schema
SET search_path TO identity_schema;

-- Create user_roles table
CREATE TABLE IF NOT EXISTS user_roles (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for user lookups
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);

-- Unique constraint: one role per user (no duplicate roles)
ALTER TABLE user_roles ADD CONSTRAINT uq_user_roles_user_role UNIQUE (user_id, role);

-- Backfill: assign default 'MEMBER' role to all existing users
INSERT INTO user_roles (id, user_id, role, created_at)
SELECT
    gen_random_uuid()::varchar(36),
    u.id,
    'MEMBER',
    NOW()
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id
);

-- Reset search path
SET search_path TO public;

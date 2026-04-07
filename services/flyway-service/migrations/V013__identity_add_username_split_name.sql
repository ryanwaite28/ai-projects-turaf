-- Service: identity-service
-- Schema: identity_schema
-- Description: Add username column, split name into first_name + last_name
-- Related: D1 (User Model Field Mismatch)

-- Set search path to target schema
SET search_path TO identity_schema;

-- Add username column
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(50);

-- Add first_name and last_name columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name VARCHAR(50);

-- Backfill: generate username from email (part before @)
UPDATE users SET username = SPLIT_PART(email, '@', 1) WHERE username IS NULL;

-- Backfill: split existing name into first_name and last_name
UPDATE users SET
    first_name = CASE
        WHEN POSITION(' ' IN COALESCE(name, '')) > 0 THEN SUBSTRING(name FROM 1 FOR POSITION(' ' IN name) - 1)
        ELSE COALESCE(name, 'Unknown')
    END,
    last_name = CASE
        WHEN POSITION(' ' IN COALESCE(name, '')) > 0 THEN SUBSTRING(name FROM POSITION(' ' IN name) + 1)
        ELSE ''
    END
WHERE first_name IS NULL;

-- Make columns NOT NULL after backfill
ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN last_name SET NOT NULL;

-- Add unique constraint on username
ALTER TABLE users ADD CONSTRAINT uq_users_username UNIQUE (username);

-- Create index for username lookups
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Drop old name column (data migrated to first_name + last_name)
ALTER TABLE users DROP COLUMN IF EXISTS name;

-- Reset search path
SET search_path TO public;

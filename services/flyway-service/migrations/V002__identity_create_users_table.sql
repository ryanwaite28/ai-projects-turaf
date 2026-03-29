-- Service: identity-service
-- Schema: identity_schema
-- Description: Create users table for authentication

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS identity_schema;

-- Set search path to target schema
SET search_path TO identity_schema;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create index on email for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Add comments for documentation
COMMENT ON TABLE users IS 'Stores user account information';
COMMENT ON COLUMN users.id IS 'Unique user identifier (UUID)';
COMMENT ON COLUMN users.email IS 'User email address (unique, normalized to lowercase)';
COMMENT ON COLUMN users.password IS 'Hashed password';
COMMENT ON COLUMN users.name IS 'User display name';
COMMENT ON COLUMN users.created_at IS 'Timestamp when user was created';
COMMENT ON COLUMN users.updated_at IS 'Timestamp when user was last updated';

-- Reset search path
SET search_path TO public;

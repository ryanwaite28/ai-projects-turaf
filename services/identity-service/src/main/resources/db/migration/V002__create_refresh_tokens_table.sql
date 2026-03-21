-- Create refresh_tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- Add foreign key constraint if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_refresh_tokens_user_id'
    ) THEN
        ALTER TABLE refresh_tokens 
        ADD CONSTRAINT fk_refresh_tokens_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- Add comments for documentation
COMMENT ON TABLE refresh_tokens IS 'Stores refresh tokens for user authentication';
COMMENT ON COLUMN refresh_tokens.id IS 'Unique refresh token identifier';
COMMENT ON COLUMN refresh_tokens.user_id IS 'Reference to user who owns this token';
COMMENT ON COLUMN refresh_tokens.token IS 'The actual refresh token value (unique)';
COMMENT ON COLUMN refresh_tokens.expires_at IS 'Timestamp when token expires';
COMMENT ON COLUMN refresh_tokens.created_at IS 'Timestamp when token was created';

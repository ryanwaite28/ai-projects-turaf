-- Service: communications-service
-- Schema: communications_schema
-- Description: Create initial tables for communications service (conversations, messages, participants, read_state)

-- ==========================================
-- Communications Schema Migration V009
-- ==========================================

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS communications_schema;

-- Set search path to target schema
SET search_path TO communications_schema;

-- ==========================================
-- Table: conversations
-- ==========================================
CREATE TABLE IF NOT EXISTS conversations (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(20) NOT NULL CHECK (type IN ('DIRECT', 'GROUP')),
    name VARCHAR(200),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Indexes for conversations
CREATE INDEX idx_conversations_type ON conversations(type);
CREATE INDEX idx_conversations_updated_at ON conversations(updated_at);

-- Comments for conversations
COMMENT ON TABLE conversations IS 'Chat conversations (direct and group)';
COMMENT ON COLUMN conversations.id IS 'Unique conversation identifier (UUID)';
COMMENT ON COLUMN conversations.type IS 'Conversation type: DIRECT (1-on-1) or GROUP (multi-user)';
COMMENT ON COLUMN conversations.name IS 'Conversation name (required for GROUP, optional for DIRECT)';
COMMENT ON COLUMN conversations.created_at IS 'Timestamp when conversation was created';
COMMENT ON COLUMN conversations.updated_at IS 'Timestamp when conversation was last updated';

-- ==========================================
-- Table: messages
-- ==========================================
CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_messages_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES conversations(id)
        ON DELETE CASCADE
);

-- Indexes for messages
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_messages_conversation_created ON messages(conversation_id, created_at DESC);

-- Comments for messages
COMMENT ON TABLE messages IS 'Individual messages within conversations';
COMMENT ON COLUMN messages.id IS 'Unique message identifier (UUID)';
COMMENT ON COLUMN messages.conversation_id IS 'Reference to parent conversation';
COMMENT ON COLUMN messages.sender_id IS 'User ID who sent the message (from identity service)';
COMMENT ON COLUMN messages.content IS 'Message content (max 10,000 characters)';
COMMENT ON COLUMN messages.created_at IS 'Timestamp when message was created';

-- ==========================================
-- Table: participants
-- ==========================================
CREATE TABLE IF NOT EXISTS participants (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('MEMBER', 'ADMIN')),
    joined_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_participants_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES conversations(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_participants_conversation_user 
        UNIQUE(conversation_id, user_id)
);

-- Indexes for participants
CREATE INDEX idx_participants_conversation_id ON participants(conversation_id);
CREATE INDEX idx_participants_user_id ON participants(user_id);

-- Comments for participants
COMMENT ON TABLE participants IS 'Users participating in conversations';
COMMENT ON COLUMN participants.id IS 'Unique participant identifier (UUID)';
COMMENT ON COLUMN participants.conversation_id IS 'Reference to conversation';
COMMENT ON COLUMN participants.user_id IS 'User ID (from identity service)';
COMMENT ON COLUMN participants.role IS 'Participant role: MEMBER or ADMIN';
COMMENT ON COLUMN participants.joined_at IS 'Timestamp when user joined conversation';

-- ==========================================
-- Table: read_state
-- ==========================================
CREATE TABLE IF NOT EXISTS read_state (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    last_read_message_id VARCHAR(36),
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_read_state_user_conversation 
        UNIQUE(user_id, conversation_id)
);

-- Indexes for read_state
CREATE INDEX idx_read_state_user_id ON read_state(user_id);
CREATE INDEX idx_read_state_conversation_id ON read_state(conversation_id);

-- Comments for read_state
COMMENT ON TABLE read_state IS 'Tracks last read message per user per conversation for unread count calculation';
COMMENT ON COLUMN read_state.id IS 'Unique read state identifier (UUID)';
COMMENT ON COLUMN read_state.user_id IS 'User ID (from identity service)';
COMMENT ON COLUMN read_state.conversation_id IS 'Reference to conversation';
COMMENT ON COLUMN read_state.last_read_message_id IS 'Last message the user has read (NULL = all messages unread)';
COMMENT ON COLUMN read_state.updated_at IS 'Timestamp when read state was last updated';

-- Reset search path
SET search_path TO public;

-- ==========================================
-- Migration Complete
-- ==========================================
-- Tables created: conversations, messages, participants, read_state
-- Indexes created: 10 indexes for query optimization
-- Foreign keys: 2 (messages -> conversations, participants -> conversations)
-- Unique constraints: 2 (participants, read_state)
-- ==========================================

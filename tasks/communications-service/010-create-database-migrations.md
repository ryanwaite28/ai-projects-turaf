# Task: Create Database Migrations

**Service**: Communications Service  
**Type**: Database Schema  
**Priority**: High  
**Estimated Time**: 2 hours  
**Dependencies**: 001-setup-project-structure

---

## Objective

Create Flyway migration scripts to set up the communications_schema with all required tables, indexes, and constraints.

---

## Acceptance Criteria

- [x] Flyway migration scripts created
- [x] All tables created with proper constraints
- [x] Indexes created for query optimization
- [x] Foreign keys configured with cascade rules
- [x] Migration runs successfully on clean database
- [x] init-db.sh updated to create schema and user

---

## Implementation

### 1. Update init-db.sh

**File**: `infrastructure/docker/postgres/init-db.sh`

Add after existing schemas:

```bash
CREATE SCHEMA IF NOT EXISTS communications_schema;

CREATE USER communications_user WITH PASSWORD 'communications_password';
GRANT ALL PRIVILEGES ON SCHEMA communications_schema TO communications_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA communications_schema TO communications_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA communications_schema TO communications_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA communications_schema GRANT ALL ON TABLES TO communications_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA communications_schema GRANT ALL ON SEQUENCES TO communications_user;
```

### 2. Create Migration Script

**File**: `services/communications-service/src/main/resources/db/migration/V001__create_communications_tables.sql`

```sql
-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS communications_schema;

-- Create conversations table
CREATE TABLE IF NOT EXISTS communications_schema.conversations (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(20) NOT NULL CHECK (type IN ('DIRECT', 'GROUP')),
    name VARCHAR(200),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_conversations_type ON communications_schema.conversations(type);
CREATE INDEX idx_conversations_updated_at ON communications_schema.conversations(updated_at);

-- Create messages table
CREATE TABLE IF NOT EXISTS communications_schema.messages (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_messages_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES communications_schema.conversations(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_messages_conversation_id ON communications_schema.messages(conversation_id);
CREATE INDEX idx_messages_created_at ON communications_schema.messages(created_at);
CREATE INDEX idx_messages_sender_id ON communications_schema.messages(sender_id);
CREATE INDEX idx_messages_conversation_created ON communications_schema.messages(conversation_id, created_at DESC);

-- Create participants table
CREATE TABLE IF NOT EXISTS communications_schema.participants (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('MEMBER', 'ADMIN')),
    joined_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_participants_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES communications_schema.conversations(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_participants_conversation_user UNIQUE(conversation_id, user_id)
);

CREATE INDEX idx_participants_conversation_id ON communications_schema.participants(conversation_id);
CREATE INDEX idx_participants_user_id ON communications_schema.participants(user_id);

-- Create read_state table
CREATE TABLE IF NOT EXISTS communications_schema.read_state (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    last_read_message_id VARCHAR(36),
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_read_state_user_conversation UNIQUE(user_id, conversation_id)
);

CREATE INDEX idx_read_state_user_id ON communications_schema.read_state(user_id);
CREATE INDEX idx_read_state_conversation_id ON communications_schema.read_state(conversation_id);

-- Add comments for documentation
COMMENT ON TABLE communications_schema.conversations IS 'Chat conversations (direct and group)';
COMMENT ON TABLE communications_schema.messages IS 'Individual messages within conversations';
COMMENT ON TABLE communications_schema.participants IS 'Users participating in conversations';
COMMENT ON TABLE communications_schema.read_state IS 'Tracks last read message per user per conversation';

COMMENT ON COLUMN communications_schema.conversations.type IS 'DIRECT (1-on-1) or GROUP (multi-user)';
COMMENT ON COLUMN communications_schema.participants.role IS 'MEMBER or ADMIN';
COMMENT ON COLUMN communications_schema.read_state.last_read_message_id IS 'Last message the user has read (null = all unread)';
```

---

## Verification

1. Start PostgreSQL:
   ```bash
   docker-compose up -d postgres
   ```

2. Run init script:
   ```bash
   docker exec -it turaf-postgres psql -U postgres -d turaf -f /docker-entrypoint-initdb.d/init-db.sh
   ```

3. Verify schema created:
   ```sql
   \dn communications_schema
   ```

4. Verify tables created:
   ```sql
   \dt communications_schema.*
   ```

5. Run service to execute Flyway migration:
   ```bash
   cd services/communications-service
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

6. Verify migration history:
   ```sql
   SELECT * FROM communications_schema.flyway_schema_history;
   ```

---

## References

- **Spec**: `specs/communications-service.md` (Database Schema section)
- **Example**: `services/experiment-service/src/main/resources/db/migration/`
- **PROJECT.md**: Section 27 (Data Architecture)

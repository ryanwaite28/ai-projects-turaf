-- Create organization_members table
CREATE TABLE IF NOT EXISTS organization_members (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    added_by VARCHAR(36) NOT NULL,
    added_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Add foreign key constraint if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_org_members_organization_id'
    ) THEN
        ALTER TABLE organization_members 
        ADD CONSTRAINT fk_org_members_organization_id 
        FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Add unique constraint if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'uq_org_members_org_user'
    ) THEN
        ALTER TABLE organization_members 
        ADD CONSTRAINT uq_org_members_org_user 
        UNIQUE (organization_id, user_id);
    END IF;
END $$;

-- Create index on organization_id for fast member lookups
CREATE INDEX IF NOT EXISTS idx_org_members_org_id ON organization_members(organization_id);

-- Create index on user_id for finding user's memberships
CREATE INDEX IF NOT EXISTS idx_org_members_user_id ON organization_members(user_id);

-- Create composite index for specific member lookup
CREATE INDEX IF NOT EXISTS idx_org_members_org_user ON organization_members(organization_id, user_id);

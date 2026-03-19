-- Create organization_members table
CREATE TABLE organization_members (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    added_by VARCHAR(36) NOT NULL,
    added_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    UNIQUE (organization_id, user_id)
);

-- Create index on organization_id for fast member lookups
CREATE INDEX idx_org_members_org_id ON organization_members(organization_id);

-- Create index on user_id for finding user's memberships
CREATE INDEX idx_org_members_user_id ON organization_members(user_id);

-- Create composite index for specific member lookup
CREATE INDEX idx_org_members_org_user ON organization_members(organization_id, user_id);

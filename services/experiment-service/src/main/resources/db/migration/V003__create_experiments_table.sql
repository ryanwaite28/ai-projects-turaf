CREATE TABLE experiments (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    hypothesis_id VARCHAR(36) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (hypothesis_id) REFERENCES hypotheses(id) ON DELETE CASCADE
);

CREATE INDEX idx_experiments_org_id ON experiments(organization_id);
CREATE INDEX idx_experiments_hypothesis_id ON experiments(hypothesis_id);
CREATE INDEX idx_experiments_status ON experiments(status);
CREATE INDEX idx_experiments_org_status ON experiments(organization_id, status);
CREATE INDEX idx_experiments_created_at ON experiments(created_at);

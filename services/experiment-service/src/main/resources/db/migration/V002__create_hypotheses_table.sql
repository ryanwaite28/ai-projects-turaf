CREATE TABLE hypotheses (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    problem_id VARCHAR(36) NOT NULL,
    statement VARCHAR(500) NOT NULL,
    expected_outcome TEXT,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE
);

CREATE INDEX idx_hypotheses_org_id ON hypotheses(organization_id);
CREATE INDEX idx_hypotheses_problem_id ON hypotheses(problem_id);
CREATE INDEX idx_hypotheses_created_at ON hypotheses(created_at);

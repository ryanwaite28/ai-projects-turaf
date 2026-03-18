-- Create metrics table for time-series data
CREATE TABLE metrics (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    experiment_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    type VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    tags JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for optimized time-series queries
CREATE INDEX idx_metrics_org_id ON metrics(organization_id);
CREATE INDEX idx_metrics_experiment_id ON metrics(experiment_id);
CREATE INDEX idx_metrics_timestamp ON metrics(timestamp DESC);
CREATE INDEX idx_metrics_experiment_name ON metrics(experiment_id, name);
CREATE INDEX idx_metrics_experiment_time ON metrics(experiment_id, timestamp DESC);
CREATE INDEX idx_metrics_org_time ON metrics(organization_id, timestamp DESC);

-- Add comments for documentation
COMMENT ON TABLE metrics IS 'Time-series metrics data for experiments';
COMMENT ON COLUMN metrics.id IS 'Unique identifier for the metric';
COMMENT ON COLUMN metrics.organization_id IS 'Organization that owns this metric';
COMMENT ON COLUMN metrics.experiment_id IS 'Experiment this metric belongs to';
COMMENT ON COLUMN metrics.name IS 'Metric name (e.g., response_time, error_rate)';
COMMENT ON COLUMN metrics.value IS 'Numeric value of the metric';
COMMENT ON COLUMN metrics.type IS 'Metric type: COUNTER, GAUGE, or HISTOGRAM';
COMMENT ON COLUMN metrics.timestamp IS 'When the metric was recorded';
COMMENT ON COLUMN metrics.tags IS 'Additional metadata as JSON key-value pairs';
COMMENT ON COLUMN metrics.created_at IS 'When the record was created in the database';

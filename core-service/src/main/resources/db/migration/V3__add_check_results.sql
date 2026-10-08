CREATE TABLE IF NOT EXISTS check_results (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    monitor_id UUID NOT NULL,
    success BOOLEAN NOT NULL,
    latency_ms BIGINT NOT NULL,
    status_code INT,
    error VARCHAR(2048),
    checked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_check_results_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_check_results_monitor FOREIGN KEY (monitor_id) REFERENCES monitors(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_check_results_lookup ON check_results(tenant_id, monitor_id, checked_at DESC);

ALTER TABLE monitors ADD COLUMN IF NOT EXISTS last_checked_at TIMESTAMP WITH TIME ZONE;

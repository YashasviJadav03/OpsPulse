CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    plan VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS monitors (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    method VARCHAR(10) NOT NULL DEFAULT 'GET',
    interval_seconds INT NOT NULL,
    expected_status INT NOT NULL DEFAULT 200,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_monitors_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_monitors_tenant_id ON monitors(tenant_id);

CREATE TABLE IF NOT EXISTS incidents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    monitor_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE,
    summary TEXT,
    CONSTRAINT fk_incidents_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_incidents_monitor FOREIGN KEY (monitor_id) REFERENCES monitors(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_incidents_tenant_id ON incidents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_incidents_monitor_id ON incidents(monitor_id);

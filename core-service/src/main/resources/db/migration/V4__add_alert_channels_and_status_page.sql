CREATE TABLE IF NOT EXISTS alert_channels (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    target VARCHAR(1024) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alert_channels_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_alert_channels_tenant ON alert_channels(tenant_id);

CREATE TABLE IF NOT EXISTS notification_logs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    channel_id UUID NOT NULL,
    incident_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    attempts INT NOT NULL DEFAULT 1,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_logs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

ALTER TABLE tenants ADD COLUMN IF NOT EXISTS slug VARCHAR(255);
CREATE UNIQUE INDEX IF NOT EXISTS idx_tenants_slug ON tenants(slug);

ALTER TABLE monitors ADD COLUMN IF NOT EXISTS public BOOLEAN NOT NULL DEFAULT FALSE;

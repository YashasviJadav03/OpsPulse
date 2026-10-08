package com.opspulse.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert_channels")
public class AlertChannel {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "type", nullable = false, length = 50)
    private String type; // WEBHOOK, EMAIL_LOG

    @Column(name = "target", nullable = false, length = 1024)
    private String target;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AlertChannel() {
    }

    public AlertChannel(UUID id, UUID tenantId, String type, String target, boolean enabled, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.type = type != null ? type.toUpperCase() : "EMAIL_LOG";
        this.target = target;
        this.enabled = enabled;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
}

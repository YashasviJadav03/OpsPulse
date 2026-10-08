package com.opspulse.core.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "monitor_id", nullable = false)
    private UUID monitorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IncidentStatus status;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    public Incident(UUID id, UUID tenantId, UUID monitorId, IncidentStatus status, String summary) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.monitorId = monitorId;
        this.status = status != null ? status : IncidentStatus.OPEN;
        this.openedAt = Instant.now();
        this.summary = summary;
    }

    public Incident(UUID tenantId, UUID monitorId, IncidentStatus status, String summary) {
        this(UUID.randomUUID(), tenantId, monitorId, status, summary);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getMonitorId() {
        return monitorId;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}

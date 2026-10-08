package com.opspulse.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "check_results")
public class CheckResultEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "monitor_id", nullable = false, updatable = false)
    private UUID monitorId;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "error", length = 2048)
    private String error;

    @Column(name = "checked_at", nullable = false, updatable = false)
    private Instant checkedAt;

    protected CheckResultEntity() {
    }

    public CheckResultEntity(UUID id, UUID tenantId, UUID monitorId, boolean success,
                             long latencyMs, Integer statusCode, String error, Instant checkedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.monitorId = monitorId;
        this.success = success;
        this.latencyMs = latencyMs;
        this.statusCode = statusCode;
        this.error = error;
        this.checkedAt = checkedAt != null ? checkedAt : Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getMonitorId() { return monitorId; }
    public boolean isSuccess() { return success; }
    public long getLatencyMs() { return latencyMs; }
    public Integer getStatusCode() { return statusCode; }
    public String getError() { return error; }
    public Instant getCheckedAt() { return checkedAt; }
}

package com.opspulse.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_logs")
public class NotificationLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected NotificationLog() {
    }

    public NotificationLog(UUID id, UUID tenantId, UUID channelId, UUID incidentId,
                           String status, int attempts, String errorMessage, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.channelId = channelId;
        this.incidentId = incidentId;
        this.status = status;
        this.attempts = attempts;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getChannelId() { return channelId; }
    public UUID getIncidentId() { return incidentId; }
    public String getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
}

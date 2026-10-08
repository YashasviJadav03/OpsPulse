package com.opspulse.core.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monitors")
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    @Column(name = "method", nullable = false, length = 10)
    private String method;

    @Column(name = "interval_seconds", nullable = false)
    private Integer intervalSeconds;

    @Column(name = "expected_status", nullable = false)
    private Integer expectedStatus;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "public", nullable = false)
    private boolean isPublic = false;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Monitor() {
    }

    public Monitor(UUID tenantId, String name, String url, String method,
                   Integer intervalSeconds, Integer expectedStatus, boolean enabled) {
        this(tenantId, name, url, method, intervalSeconds, expectedStatus, enabled, false);
    }

    public Monitor(UUID tenantId, String name, String url, String method,
                   Integer intervalSeconds, Integer expectedStatus, boolean enabled, boolean isPublic) {
        this.tenantId = tenantId;
        this.name = name;
        this.url = url;
        this.method = method != null ? method.toUpperCase() : "GET";
        this.intervalSeconds = intervalSeconds;
        this.expectedStatus = expectedStatus != null ? expectedStatus : 200;
        this.enabled = enabled;
        this.isPublic = isPublic;
        this.createdAt = Instant.now();
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(Instant lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method != null ? method.toUpperCase() : "GET";
    }

    public Integer getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(Integer intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public Integer getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(Integer expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package com.opspulse.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "plan", nullable = false, length = 50)
    private String plan;

    @Column(name = "slug", unique = true)
    private String slug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Tenant() {
    }

    public Tenant(UUID id, String name, String plan, Instant createdAt) {
        this(id, name, plan, null, createdAt);
    }

    public Tenant(UUID id, String name, String plan, String slug, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.name = name;
        this.plan = plan;
        this.slug = slug != null ? slug : generateSlug(name);
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    private static String generateSlug(String name) {
        if (name == null) return null;
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

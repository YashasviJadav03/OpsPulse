package com.opspulse.core.repository;

import com.opspulse.core.domain.Monitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, UUID> {

    @Query("SELECT m FROM Monitor m WHERE m.id = :id")
    Optional<Monitor> findById(@Param("id") UUID id);

    List<Monitor> findByEnabledTrue();

    @Query("SELECT m FROM Monitor m WHERE m.enabled = true AND (m.lastCheckedAt IS NULL OR m.lastCheckedAt <= :cutoff)")
    List<Monitor> findDueMonitors(@Param("cutoff") java.time.Instant cutoff);

    List<Monitor> findAllByTenantIdAndIsPublicTrue(UUID tenantId);
}

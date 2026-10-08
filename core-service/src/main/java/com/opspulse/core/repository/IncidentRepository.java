package com.opspulse.core.repository;

import com.opspulse.core.domain.Incident;
import com.opspulse.core.domain.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    List<Incident> findByStatus(IncidentStatus status);
    Optional<Incident> findByMonitorIdAndStatus(UUID monitorId, IncidentStatus status);
    List<Incident> findAllByTenantId(UUID tenantId);
    List<Incident> findAllByTenantIdAndStatus(UUID tenantId, IncidentStatus status);
    Optional<Incident> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Incident> findFirstByTenantIdAndMonitorIdAndStatusInOrderByOpenedAtDesc(UUID tenantId, UUID monitorId, List<IncidentStatus> statuses);
}

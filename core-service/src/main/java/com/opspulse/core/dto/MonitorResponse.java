package com.opspulse.core.dto;

import com.opspulse.core.domain.Monitor;

import java.time.Instant;
import java.util.UUID;

public record MonitorResponse(
        UUID id,
        UUID tenantId,
        String name,
        String url,
        String method,
        Integer intervalSeconds,
        Integer expectedStatus,
        boolean enabled,
        Instant createdAt
) {
    public static MonitorResponse fromEntity(Monitor monitor) {
        return new MonitorResponse(
                monitor.getId(),
                monitor.getTenantId(),
                monitor.getName(),
                monitor.getUrl(),
                monitor.getMethod(),
                monitor.getIntervalSeconds(),
                monitor.getExpectedStatus(),
                monitor.isEnabled(),
                monitor.getCreatedAt()
        );
    }
}

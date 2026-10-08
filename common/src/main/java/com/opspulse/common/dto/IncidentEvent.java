package com.opspulse.common.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record IncidentEvent(
        UUID incidentId,
        UUID tenantId,
        UUID monitorId,
        String status,
        String summary,
        Instant timestamp
) implements Serializable {}

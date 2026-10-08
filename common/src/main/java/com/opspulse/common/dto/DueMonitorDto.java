package com.opspulse.common.dto;

import java.io.Serializable;
import java.util.UUID;

public record DueMonitorDto(
        UUID id,
        UUID tenantId,
        String name,
        String url,
        String method,
        int intervalSeconds,
        int expectedStatus
) implements Serializable {}

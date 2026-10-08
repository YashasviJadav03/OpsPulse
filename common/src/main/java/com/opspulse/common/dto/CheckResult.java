package com.opspulse.common.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record CheckResult(
        UUID monitorId,
        UUID tenantId,
        Integer statusCode,
        long latencyMs,
        boolean success,
        String error,
        Instant checkedAt
) implements Serializable {}

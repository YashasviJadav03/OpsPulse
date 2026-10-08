package com.opspulse.common.dto;

import java.time.Instant;

/**
 * Standard API error payload returned by @RestControllerAdvice across services.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path);
    }
}

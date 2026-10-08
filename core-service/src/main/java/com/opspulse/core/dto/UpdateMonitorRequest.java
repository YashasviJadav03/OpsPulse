package com.opspulse.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMonitorRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @NotBlank(message = "URL is required")
        @Pattern(regexp = "^https?://.+", message = "URL must be a valid HTTP or HTTPS address")
        String url,

        String method,

        @NotNull(message = "Interval seconds is required")
        @Min(value = 30, message = "Interval must be at least 30 seconds")
        @Max(value = 3600, message = "Interval must be at most 3600 seconds")
        Integer intervalSeconds,

        @NotNull(message = "Expected status is required")
        @Min(value = 100, message = "Expected status must be a valid HTTP status (>= 100)")
        @Max(value = 599, message = "Expected status must be a valid HTTP status (<= 599)")
        Integer expectedStatus,

        @NotNull(message = "Enabled flag is required")
        Boolean enabled
) {
    public String resolveMethod() {
        return (method == null || method.isBlank()) ? "GET" : method.trim().toUpperCase();
    }
}

package com.opspulse.core.dto;

import com.opspulse.core.domain.Role;

import java.util.UUID;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UUID userId,
        UUID tenantId,
        String email,
        Role role
) {
    public static AuthResponse of(String token, long expiresIn, UUID userId, UUID tenantId, String email, Role role) {
        return new AuthResponse(token, "Bearer", expiresIn, userId, tenantId, email, role);
    }
}

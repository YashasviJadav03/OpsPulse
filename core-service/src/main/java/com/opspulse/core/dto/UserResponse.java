package com.opspulse.core.dto;

import com.opspulse.core.domain.Role;
import com.opspulse.core.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID tenantId,
        String email,
        Role role,
        Instant createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}

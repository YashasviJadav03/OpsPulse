package com.opspulse.core.service;

import com.opspulse.common.exception.BadRequestException;
import com.opspulse.common.tenant.TenantContext;
import com.opspulse.core.domain.Role;
import com.opspulse.core.domain.User;
import com.opspulse.core.dto.CreateUserRequest;
import com.opspulse.core.dto.UserResponse;
import com.opspulse.core.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        if (userRepository.existsByTenantIdAndEmail(tenantId, request.email())) {
            throw new BadRequestException("User already exists with email: " + request.email());
        }

        String passwordHash = passwordEncoder.encode(request.password());
        Role role = request.role() != null ? request.role() : Role.MEMBER;
        User user = new User(tenantId, request.email(), passwordHash, role);
        User savedUser = userRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        UUID tenantId = TenantContext.requireTenantId();
        return userRepository.findAllByTenantId(tenantId)
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }
}

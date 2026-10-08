package com.opspulse.core.service;

import com.opspulse.common.exception.BadRequestException;
import com.opspulse.common.exception.UnauthorizedException;
import com.opspulse.common.tenant.TenantContext;
import com.opspulse.core.domain.Role;
import com.opspulse.core.domain.Tenant;
import com.opspulse.core.domain.User;
import com.opspulse.core.dto.AuthResponse;
import com.opspulse.core.dto.LoginRequest;
import com.opspulse.core.dto.RegisterTenantRequest;
import com.opspulse.core.repository.TenantRepository;
import com.opspulse.core.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(TenantRepository tenantRepository,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse registerTenant(RegisterTenantRequest request) {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant(tenantId, request.tenantName(), request.plan(), Instant.now());
        tenantRepository.save(tenant);

        TenantContext.setTenantId(tenantId);
        try {
            if (userRepository.existsByTenantIdAndEmail(tenantId, request.email())) {
                throw new BadRequestException("User already exists with email: " + request.email());
            }

            String passwordHash = passwordEncoder.encode(request.password());
            User user = new User(tenantId, request.email(), passwordHash, Role.ADMIN);
            User savedUser = userRepository.save(user);

            String token = jwtService.generateToken(savedUser.getId(), tenantId, savedUser.getRole());
            return AuthResponse.of(token, jwtService.getExpirationSeconds(), savedUser.getId(), tenantId, savedUser.getEmail(), savedUser.getRole());
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user;
        if (request.tenantId() != null) {
            user = userRepository.findByTenantIdAndEmail(request.tenantId(), request.email())
                    .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        } else {
            user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getTenantId(), user.getRole());
        return AuthResponse.of(token, jwtService.getExpirationSeconds(), user.getId(), user.getTenantId(), user.getEmail(), user.getRole());
    }
}

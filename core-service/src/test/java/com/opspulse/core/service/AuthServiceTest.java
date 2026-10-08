package com.opspulse.core.service;

import com.opspulse.common.exception.BadRequestException;
import com.opspulse.common.exception.UnauthorizedException;
import com.opspulse.core.domain.Role;
import com.opspulse.core.domain.Tenant;
import com.opspulse.core.domain.User;
import com.opspulse.core.dto.AuthResponse;
import com.opspulse.core.dto.LoginRequest;
import com.opspulse.core.dto.RegisterTenantRequest;
import com.opspulse.core.repository.TenantRepository;
import com.opspulse.core.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    private final String secret = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256-tests!";
    private final long expirationSeconds = 3600;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService(secret, expirationSeconds);
        authService = new AuthService(tenantRepository, userRepository, passwordEncoder, jwtService);
    }

    @Test
    void shouldRegisterTenantAndHashPassword() {
        RegisterTenantRequest request = new RegisterTenantRequest(
                "Acme Corp",
                "PRO",
                "admin@acme.com",
                "securePassword123"
        );

        when(userRepository.existsByTenantIdAndEmail(any(UUID.class), eq("admin@acme.com"))).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.registerTenant(request);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("admin@acme.com");
        assertThat(response.role()).isEqualTo(Role.ADMIN);
        assertThat(response.token()).isNotBlank();

        // Verify password hashing
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("securePassword123");
        assertThat(passwordEncoder.matches("securePassword123", savedUser.getPasswordHash())).isTrue();

        // Verify JWT claims
        Claims claims = jwtService.extractClaims(response.token());
        assertThat(claims.get("tenant_id")).isEqualTo(response.tenantId().toString());
        assertThat(claims.get("role")).isEqualTo("ADMIN");
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    void shouldRejectDuplicateEmailInTenant() {
        RegisterTenantRequest request = new RegisterTenantRequest(
                "Acme Corp",
                "PRO",
                "admin@acme.com",
                "securePassword123"
        );

        when(userRepository.existsByTenantIdAndEmail(any(UUID.class), eq("admin@acme.com"))).thenReturn(true);

        assertThatThrownBy(() -> authService.registerTenant(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User already exists with email: admin@acme.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLoginSuccessfullyWithCorrectJwtClaims() {
        UUID tenantId = UUID.randomUUID();
        String rawPassword = "mypassword123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(tenantId, "user@example.com", encodedPassword, Role.MEMBER);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("user@example.com", rawPassword, null);
        AuthResponse response = authService.login(request);

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.tenantId()).isEqualTo(tenantId);
        assertThat(response.role()).isEqualTo(Role.MEMBER);

        // Verify JWT claims
        Claims claims = jwtService.extractClaims(response.token());
        assertThat(claims.get("tenant_id")).isEqualTo(tenantId.toString());
        assertThat(claims.get("role")).isEqualTo("MEMBER");
        long diffSeconds = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000;
        assertThat(diffSeconds).isEqualTo(3600);
    }

    @Test
    void shouldRejectLoginWithInvalidPassword() {
        UUID tenantId = UUID.randomUUID();
        User user = new User(tenantId, "user@example.com", passwordEncoder.encode("correctPassword"), Role.MEMBER);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("user@example.com", "wrongPassword", null);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void shouldRejectLoginWithNonExistentEmail() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("unknown@example.com", "password", null);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }
}

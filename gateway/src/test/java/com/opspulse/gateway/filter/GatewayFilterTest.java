package com.opspulse.gateway.filter;

import com.opspulse.gateway.ratelimit.TokenBucketRateLimiter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayFilterTest {

    private final String secret = "opspulse-default-secret-key-that-is-at-least-256-bits-long-32bytes!";
    private SecretKey key;
    private JwtAuthenticationFilter jwtFilter;
    private RateLimitFilter rateLimitFilter;
    private TokenBucketRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        jwtFilter = new JwtAuthenticationFilter(secret);
        rateLimiter = new TokenBucketRateLimiter(2.0, 2.0); // low capacity for rate limit testing
        rateLimitFilter = new RateLimitFilter(rateLimiter);
    }

    private String createValidToken(UUID userId, UUID tenantId, String role) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("tenant_id", tenantId.toString())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();
    }

    @Test
    void shouldReturn401WhenTokenIsMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/monitors").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        jwtFilter.filter(exchange, chain).block();

        assertThat(chainInvoked.get()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401WhenTokenIsTampered() {
        UUID tenantId = UUID.randomUUID();
        String validToken = createValidToken(UUID.randomUUID(), tenantId, "ADMIN");
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "xyz12";

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/monitors")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        jwtFilter.filter(exchange, chain).block();

        assertThat(chainInvoked.get()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldOverwriteClientSuppliedTenantHeaderWithJwtClaim() {
        UUID actualTenantId = UUID.randomUUID();
        UUID spoofedTenantId = UUID.randomUUID();
        String token = createValidToken(UUID.randomUUID(), actualTenantId, "ADMIN");

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/monitors")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Tenant-Id", spoofedTenantId.toString())
                .header("X-User-Role", "SUPER_ADMIN")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> forwardedTenantHeader = new AtomicReference<>();
        AtomicReference<String> forwardedRoleHeader = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            forwardedTenantHeader.set(ex.getRequest().getHeaders().getFirst("X-Tenant-Id"));
            forwardedRoleHeader.set(ex.getRequest().getHeaders().getFirst("X-User-Role"));
            return Mono.empty();
        };

        jwtFilter.filter(exchange, chain).block();

        assertThat(forwardedTenantHeader.get()).isEqualTo(actualTenantId.toString());
        assertThat(forwardedTenantHeader.get()).isNotEqualTo(spoofedTenantId.toString());
        assertThat(forwardedRoleHeader.get()).isEqualTo("ADMIN");
    }

    @Test
    void shouldForwardValidTokenHeadersToDownstream() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String token = createValidToken(userId, tenantId, "MEMBER");

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/monitors")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> tenantHeader = new AtomicReference<>();
        AtomicReference<String> roleHeader = new AtomicReference<>();
        AtomicReference<String> userHeader = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            tenantHeader.set(ex.getRequest().getHeaders().getFirst("X-Tenant-Id"));
            roleHeader.set(ex.getRequest().getHeaders().getFirst("X-User-Role"));
            userHeader.set(ex.getRequest().getHeaders().getFirst("X-User-Id"));
            return Mono.empty();
        };

        jwtFilter.filter(exchange, chain).block();

        assertThat(tenantHeader.get()).isEqualTo(tenantId.toString());
        assertThat(roleHeader.get()).isEqualTo("MEMBER");
        assertThat(userHeader.get()).isEqualTo(userId.toString());
    }

    @Test
    void shouldAllowPublicAuthEndpointsWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        jwtFilter.filter(exchange, chain).block();

        assertThat(chainInvoked.get()).isTrue();
    }

    @Test
    void shouldReturn429WhenRateLimitExceeded() {
        UUID tenantId = UUID.randomUUID();
        String token = createValidToken(UUID.randomUUID(), tenantId, "ADMIN");

        GatewayFilterChain successChain = ex -> Mono.empty();

        // Capacity is 2
        // Request 1: allowed
        MockServerWebExchange ex1 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/monitors").header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());
        jwtFilter.filter(ex1, passEx -> rateLimitFilter.filter(passEx, successChain)).block();
        assertThat(ex1.getResponse().getStatusCode()).isNull(); // null means passed through to chain

        // Request 2: allowed
        MockServerWebExchange ex2 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/monitors").header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());
        jwtFilter.filter(ex2, passEx -> rateLimitFilter.filter(passEx, successChain)).block();
        assertThat(ex2.getResponse().getStatusCode()).isNull();

        // Request 3: rate limit exceeded -> 429
        MockServerWebExchange ex3 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/monitors").header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());
        jwtFilter.filter(ex3, passEx -> rateLimitFilter.filter(passEx, successChain)).block();
        assertThat(ex3.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(ex3.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("60");
    }
}

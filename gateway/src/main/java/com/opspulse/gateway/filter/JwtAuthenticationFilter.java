package com.opspulse.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String TENANT_ID_ATTR = "opspulse.tenant_id";
    public static final String USER_ROLE_ATTR = "opspulse.user_role";

    private final SecretKey key;

    public JwtAuthenticationFilter(
            @Value("${opspulse.jwt.secret:opspulse-default-secret-key-that-is-at-least-256-bits-long-32bytes!}") String secret
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. Always strip incoming client-supplied tenant and role headers to prevent spoofing
        ServerHttpRequest requestWithoutClientHeaders = exchange.getRequest().mutate()
                .headers(httpHeaders -> {
                    httpHeaders.remove("X-Tenant-Id");
                    httpHeaders.remove("X-User-Role");
                    httpHeaders.remove("X-User-Id");
                })
                .build();
        ServerWebExchange cleanedExchange = exchange.mutate().request(requestWithoutClientHeaders).build();

        // 2. Allow public / auth endpoints and actuator without token
        if (isPublicPath(path)) {
            return chain.filter(cleanedExchange);
        }

        // 3. Extract Authorization header
        String authHeader = cleanedExchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return writeUnauthorizedResponse(cleanedExchange, "Missing or malformed Authorization header");
        }

        String token = authHeader.substring(7).trim();
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return writeUnauthorizedResponse(cleanedExchange, "Invalid or expired JWT token");
        }

        String tenantIdStr = claims.get("tenant_id", String.class);
        String roleStr = claims.get("role", String.class);
        String userId = claims.getSubject();

        if (tenantIdStr == null || roleStr == null) {
            return writeUnauthorizedResponse(cleanedExchange, "JWT token missing required claims");
        }

        UUID tenantId;
        try {
            tenantId = UUID.fromString(tenantIdStr);
        } catch (IllegalArgumentException e) {
            return writeUnauthorizedResponse(cleanedExchange, "Invalid tenant UUID in JWT token");
        }

        // Store in exchange attributes for downstream filters
        cleanedExchange.getAttributes().put(TENANT_ID_ATTR, tenantId);
        cleanedExchange.getAttributes().put(USER_ROLE_ATTR, roleStr);

        // 4. Mutate request: forward the verified X-Tenant-Id, X-User-Role, X-User-Id
        ServerHttpRequest mutatedRequest = cleanedExchange.getRequest().mutate()
                .header("X-Tenant-Id", tenantIdStr)
                .header("X-User-Role", roleStr)
                .header("X-User-Id", userId != null ? userId : "")
                .build();

        return chain.filter(cleanedExchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith("/auth/") || path.startsWith("/actuator") || path.startsWith("/public/");
    }

    private Mono<Void> writeUnauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\",\"path\":\"%s\"}",
                message, exchange.getRequest().getURI().getPath());
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}

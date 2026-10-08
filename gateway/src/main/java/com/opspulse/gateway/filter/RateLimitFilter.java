package com.opspulse.gateway.filter;

import com.opspulse.gateway.ratelimit.TokenBucketRateLimiter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final TokenBucketRateLimiter rateLimiter;

    public RateLimitFilter(TokenBucketRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        UUID tenantId = exchange.getAttribute(JwtAuthenticationFilter.TENANT_ID_ATTR);

        if (tenantId == null) {
            String tenantHeader = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                try {
                    tenantId = UUID.fromString(tenantHeader.trim());
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (tenantId != null && !rateLimiter.tryConsume(tenantId)) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            response.getHeaders().set("Retry-After", "60");

            String body = String.format("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded for tenant\",\"path\":\"%s\"}",
                    exchange.getRequest().getURI().getPath());
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -50;
    }
}

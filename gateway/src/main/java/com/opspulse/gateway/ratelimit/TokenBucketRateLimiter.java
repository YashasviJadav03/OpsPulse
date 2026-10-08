package com.opspulse.gateway.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBucketRateLimiter {

    private final double capacity;
    private final double refillTokensPerMinute;
    private final Map<UUID, TokenBucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(
            @Value("${opspulse.ratelimit.capacity:100}") double capacity,
            @Value("${opspulse.ratelimit.refill-tokens-per-minute:100}") double refillTokensPerMinute
    ) {
        this.capacity = capacity;
        this.refillTokensPerMinute = refillTokensPerMinute;
    }

    public boolean tryConsume(UUID tenantId) {
        if (tenantId == null) {
            return true;
        }
        TokenBucket bucket = buckets.computeIfAbsent(tenantId, id -> new TokenBucket(capacity, refillTokensPerMinute));
        return bucket.tryConsume();
    }

    public void reset() {
        buckets.clear();
    }
}

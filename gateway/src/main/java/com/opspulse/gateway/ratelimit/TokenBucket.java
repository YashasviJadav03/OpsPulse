package com.opspulse.gateway.ratelimit;

/**
 * Thread-safe token bucket algorithm for in-memory rate limiting.
 */
public class TokenBucket {

    private final double capacity;
    private final double refillTokensPerSecond;
    private double availableTokens;
    private long lastRefillNanos;

    public TokenBucket(double capacity, double refillTokensPerMinute) {
        this.capacity = capacity;
        this.refillTokensPerSecond = refillTokensPerMinute / 60.0;
        this.availableTokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    public synchronized boolean tryConsume() {
        return tryConsume(1.0);
    }

    public synchronized boolean tryConsume(double tokens) {
        refill();
        if (availableTokens >= tokens) {
            availableTokens -= tokens;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.nanoTime();
        double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
        if (elapsedSeconds > 0) {
            availableTokens = Math.min(capacity, availableTokens + (elapsedSeconds * refillTokensPerSecond));
            lastRefillNanos = now;
        }
    }

    public synchronized double getAvailableTokens() {
        refill();
        return availableTokens;
    }
}

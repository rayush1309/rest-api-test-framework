package com.sdetlabs.framework.resilience;

import java.time.Duration;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Non-blocking token-bucket limiter for controlling outbound request volume. */
public final class RequestRateLimiter {
    private final int capacity;
    private final long refillPeriodNanos;
    private final LongSupplier nanoTime;
    private double availableTokens;
    private long lastRefillNanos;

    public RequestRateLimiter(int permitsPerPeriod, Duration refillPeriod) {
        this(permitsPerPeriod, refillPeriod, System::nanoTime);
    }

    public RequestRateLimiter(int permitsPerPeriod, Duration refillPeriod, LongSupplier nanoTime) {
        if (permitsPerPeriod < 1) throw new IllegalArgumentException("permitsPerPeriod must be at least 1");
        Objects.requireNonNull(refillPeriod, "refillPeriod must not be null");
        if (refillPeriod.isNegative() || refillPeriod.isZero()) {
            throw new IllegalArgumentException("refillPeriod must be positive");
        }
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime must not be null");
        capacity = permitsPerPeriod;
        refillPeriodNanos = refillPeriod.toNanos();
        availableTokens = capacity;
        lastRefillNanos = nanoTime.getAsLong();
    }

    public synchronized boolean tryAcquire() {
        refill();
        if (availableTokens < 1.0) return false;
        availableTokens -= 1.0;
        return true;
    }

    private void refill() {
        long now = nanoTime.getAsLong();
        long elapsed = now - lastRefillNanos;
        if (elapsed <= 0) return;
        double refillRatePerNano = (double) capacity / refillPeriodNanos;
        availableTokens = Math.min(capacity, availableTokens + elapsed * refillRatePerNano);
        lastRefillNanos = now;
    }
}

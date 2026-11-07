package com.sdetlabs.framework.resilience;

import java.time.Duration;
import java.util.Objects;

/** Immutable retry timing and attempt configuration. */
public record RetryPolicy(int maxAttempts, Duration initialDelay, double multiplier, Duration maxDelay) {
    public RetryPolicy {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be at least 1");
        Objects.requireNonNull(initialDelay, "initialDelay must not be null");
        Objects.requireNonNull(maxDelay, "maxDelay must not be null");
        if (initialDelay.isNegative() || maxDelay.isNegative()) {
            throw new IllegalArgumentException("Retry delays cannot be negative");
        }
        if (!Double.isFinite(multiplier) || multiplier < 1.0) {
            throw new IllegalArgumentException("multiplier must be a finite number greater than or equal to 1");
        }
    }

    public Duration delayBeforeRetry(int retryNumber) {
        if (retryNumber < 0) throw new IllegalArgumentException("retryNumber cannot be negative");
        double delayMillis = initialDelay.toMillis() * Math.pow(multiplier, retryNumber);
        long bounded = Math.min((long) Math.min(delayMillis, Long.MAX_VALUE), maxDelay.toMillis());
        return Duration.ofMillis(bounded);
    }
}

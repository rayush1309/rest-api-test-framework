package com.sdetlabs.framework.resilience;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Limits concurrent work so one slow dependency cannot consume every caller thread. */
public final class Bulkhead {
    private final Semaphore permits;
    private final Duration maxWait;

    public Bulkhead(int maxConcurrentCalls, Duration maxWait) {
        if (maxConcurrentCalls < 1) throw new IllegalArgumentException("maxConcurrentCalls must be at least 1");
        this.maxWait = Objects.requireNonNull(maxWait, "maxWait must not be null");
        if (maxWait.isNegative()) throw new IllegalArgumentException("maxWait cannot be negative");
        permits = new Semaphore(maxConcurrentCalls, true);
    }

    public <T> T execute(Supplier<T> operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        boolean acquired;
        try {
            acquired = permits.tryAcquire(maxWait.toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BulkheadRejectedException("Interrupted while waiting for a bulkhead permit", exception);
        }
        if (!acquired) throw new BulkheadRejectedException("Bulkhead is full; request was rejected");
        try {
            return operation.get();
        } finally {
            permits.release();
        }
    }

    public int availablePermits() {
        return permits.availablePermits();
    }
}

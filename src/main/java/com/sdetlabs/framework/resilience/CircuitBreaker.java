package com.sdetlabs.framework.resilience;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/** Thread-safe circuit breaker with closed, open, and half-open states. */
public final class CircuitBreaker {
    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final Duration openDuration;
    private final Clock clock;
    private State state = State.CLOSED;
    private int consecutiveFailures;
    private Instant reopenAt;
    private boolean probeInFlight;

    public CircuitBreaker(int failureThreshold, Duration openDuration) {
        this(failureThreshold, openDuration, Clock.systemUTC());
    }

    public CircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
        if (failureThreshold < 1) throw new IllegalArgumentException("failureThreshold must be at least 1");
        this.openDuration = Objects.requireNonNull(openDuration, "openDuration must not be null");
        if (openDuration.isNegative() || openDuration.isZero()) {
            throw new IllegalArgumentException("openDuration must be positive");
        }
        this.failureThreshold = failureThreshold;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public <T> T execute(Supplier<T> operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        boolean probe = acquirePermission();
        try {
            T result = operation.get();
            recordSuccess(probe);
            return result;
        } catch (RuntimeException exception) {
            recordFailure(probe);
            throw exception;
        }
    }

    public synchronized State state() {
        transitionToHalfOpenIfReady();
        return state;
    }

    public synchronized int consecutiveFailures() {
        return consecutiveFailures;
    }

    private synchronized boolean acquirePermission() {
        transitionToHalfOpenIfReady();
        if (state == State.OPEN) {
            throw new CircuitOpenException("Circuit is open until " + reopenAt);
        }
        if (state == State.HALF_OPEN) {
            if (probeInFlight) throw new CircuitOpenException("Circuit half-open probe is already in flight");
            probeInFlight = true;
            return true;
        }
        return false;
    }

    private synchronized void recordSuccess(boolean probe) {
        if (probe || state == State.HALF_OPEN) {
            state = State.CLOSED;
            consecutiveFailures = 0;
            reopenAt = null;
            probeInFlight = false;
        } else if (state == State.CLOSED) {
            consecutiveFailures = 0;
        }
    }

    private synchronized void recordFailure(boolean probe) {
        if (probe || state == State.HALF_OPEN) {
            openCircuit();
            return;
        }
        if (state == State.CLOSED && ++consecutiveFailures >= failureThreshold) openCircuit();
    }

    private void openCircuit() {
        state = State.OPEN;
        reopenAt = clock.instant().plus(openDuration);
        probeInFlight = false;
    }

    private void transitionToHalfOpenIfReady() {
        if (state == State.OPEN && !clock.instant().isBefore(reopenAt)) {
            state = State.HALF_OPEN;
            probeInFlight = false;
        }
    }
}

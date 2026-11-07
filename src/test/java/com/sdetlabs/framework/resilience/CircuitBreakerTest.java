package com.sdetlabs.framework.resilience;

import org.testng.annotations.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

public class CircuitBreakerTest {
    @Test(groups = {"resilience", "positive", "regression"})
    public void opensAfterFailuresAndClosesAfterSuccessfulHalfOpenProbe() {
        ManualClock clock = new ManualClock(Instant.parse("2026-01-01T00:00:00Z"));
        CircuitBreaker breaker = new CircuitBreaker(2, Duration.ofSeconds(5), clock);

        assertThrows(IllegalStateException.class,
                () -> breaker.execute(() -> { throw new IllegalStateException("failure one"); }));
        assertThrows(IllegalStateException.class,
                () -> breaker.execute(() -> { throw new IllegalStateException("failure two"); }));
        assertEquals(breaker.state(), CircuitBreaker.State.OPEN);
        assertThrows(CircuitOpenException.class, () -> breaker.execute(() -> "must not run"));

        clock.advance(Duration.ofSeconds(5));
        assertEquals(breaker.state(), CircuitBreaker.State.HALF_OPEN);
        assertEquals(breaker.execute(() -> "recovered"), "recovered");
        assertEquals(breaker.state(), CircuitBreaker.State.CLOSED);
        assertEquals(breaker.consecutiveFailures(), 0);
    }

    @Test(groups = {"resilience", "negative", "regression"})
    public void reopensWhenHalfOpenProbeFails() {
        ManualClock clock = new ManualClock(Instant.EPOCH);
        CircuitBreaker breaker = new CircuitBreaker(1, Duration.ofSeconds(1), clock);
        assertThrows(IllegalStateException.class,
                () -> breaker.execute(() -> { throw new IllegalStateException("initial failure"); }));

        clock.advance(Duration.ofSeconds(1));
        assertThrows(IllegalStateException.class,
                () -> breaker.execute(() -> { throw new IllegalStateException("probe failure"); }));
        assertEquals(breaker.state(), CircuitBreaker.State.OPEN);
    }

    private static final class ManualClock extends Clock {
        private final AtomicReference<Instant> now;

        private ManualClock(Instant initialTime) {
            now = new AtomicReference<>(initialTime);
        }

        private void advance(Duration duration) {
            now.updateAndGet(current -> current.plus(duration));
        }

        @Override
        public ZoneId getZone() { return ZoneId.of("UTC"); }

        @Override
        public Clock withZone(ZoneId zone) { return this; }

        @Override
        public Instant instant() { return now.get(); }
    }
}

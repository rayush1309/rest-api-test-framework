package com.sdetlabs.framework.resilience;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Executes an operation with bounded retries for selected results or runtime failures. */
public final class RetryExecutor {
    public <T> T execute(Supplier<T> operation, Predicate<T> retryableResult,
                         Predicate<RuntimeException> retryableException, RetryPolicy policy) {
        Objects.requireNonNull(operation, "operation must not be null");
        Objects.requireNonNull(retryableResult, "retryableResult must not be null");
        Objects.requireNonNull(retryableException, "retryableException must not be null");
        Objects.requireNonNull(policy, "policy must not be null");

        for (int attempt = 1; ; attempt++) {
            try {
                T result = operation.get();
                if (attempt >= policy.maxAttempts() || !retryableResult.test(result)) return result;
            } catch (RuntimeException exception) {
                if (attempt >= policy.maxAttempts() || !retryableException.test(exception)) throw exception;
            }
            pause(policy.delayBeforeRetry(attempt - 1));
        }
    }

    private static void pause(Duration delay) {
        if (delay.isZero()) return;
        try {
            long millis = delay.toMillis();
            int nanos = (int) delay.minusMillis(millis).toNanos();
            Thread.sleep(millis, nanos);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RetryInterruptedException("Retry wait was interrupted", exception);
        }
    }

    public static final class RetryInterruptedException extends RuntimeException {
        public RetryInterruptedException(String message, InterruptedException cause) {
            super(message, cause);
        }
    }
}

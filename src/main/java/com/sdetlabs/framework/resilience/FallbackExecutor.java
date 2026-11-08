package com.sdetlabs.framework.resilience;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Runs a controlled fallback only for explicitly classified recoverable failures. */
public final class FallbackExecutor {
    public <T> T execute(Supplier<T> primary, Predicate<RuntimeException> recoverableFailure,
                         Supplier<T> fallback) {
        Objects.requireNonNull(primary, "primary must not be null");
        Objects.requireNonNull(recoverableFailure, "recoverableFailure must not be null");
        Objects.requireNonNull(fallback, "fallback must not be null");
        try {
            return primary.get();
        } catch (RuntimeException failure) {
            if (!recoverableFailure.test(failure)) throw failure;
            return fallback.get();
        }
    }
}

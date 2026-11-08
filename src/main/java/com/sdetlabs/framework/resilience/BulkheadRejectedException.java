package com.sdetlabs.framework.resilience;

/** Signals that work was rejected because the bulkhead had no available capacity. */
public final class BulkheadRejectedException extends RuntimeException {
    public BulkheadRejectedException(String message) {
        super(message);
    }

    public BulkheadRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}

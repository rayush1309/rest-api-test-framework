package com.sdetlabs.framework.concurrent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Small per-thread storage for execution data that must not leak between tests. */
public final class ThreadLocalTestContext {
    private static final ThreadLocal<Map<String, Object>> VALUES = ThreadLocal.withInitial(HashMap::new);

    private ThreadLocalTestContext() { }

    public static void put(String key, Object value) {
        VALUES.get().put(requireKey(key), Objects.requireNonNull(value, "value must not be null"));
    }

    public static <T> Optional<T> get(String key, Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        Object value = VALUES.get().get(requireKey(key));
        if (value == null) return Optional.empty();
        if (!type.isInstance(value)) {
            throw new IllegalStateException("Context value '" + key + "' is not a " + type.getName());
        }
        return Optional.of(type.cast(value));
    }

    public static void clear() {
        VALUES.remove();
    }

    private static String requireKey(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Context key must not be blank");
        return key;
    }
}

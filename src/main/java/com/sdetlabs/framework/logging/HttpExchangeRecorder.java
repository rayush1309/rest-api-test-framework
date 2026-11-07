package com.sdetlabs.framework.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Holds sanitized HTTP exchanges for the current test thread's report attachment. */
public final class HttpExchangeRecorder {
    private static final ThreadLocal<List<String>> EXCHANGES = new ThreadLocal<>();
    private static final ThreadLocal<Consumer<String>> ATTACHMENT_SINK = new ThreadLocal<>();

    private HttpExchangeRecorder() { }

    public static void beginTest(Consumer<String> attachmentSink) {
        EXCHANGES.set(new ArrayList<>());
        ATTACHMENT_SINK.set(attachmentSink);
    }

    static void record(String exchange) {
        List<String> current = EXCHANGES.get();
        if (current != null) {
            current.add(exchange);
            Consumer<String> attachmentSink = ATTACHMENT_SINK.get();
            if (attachmentSink != null) {
                attachmentSink.accept(exchange);
            }
        }
    }

    public static List<String> finishTest() {
        List<String> current = EXCHANGES.get();
        EXCHANGES.remove();
        ATTACHMENT_SINK.remove();
        return current == null ? List.of() : List.copyOf(current);
    }
}

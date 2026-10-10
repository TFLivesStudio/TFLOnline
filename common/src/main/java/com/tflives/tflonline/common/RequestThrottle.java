package com.tflives.tflonline.common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RequestThrottle {
    private final long minimumNanos;
    private final ConcurrentHashMap<String, Long> lastRequests = new ConcurrentHashMap<>();

    public RequestThrottle(long minimumMillis) {
        if (minimumMillis < 0 || minimumMillis > 60_000) throw new IllegalArgumentException("Invalid throttling window");
        minimumNanos = java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(minimumMillis);
    }

    public boolean allow(String serverName) {
        long now = System.nanoTime();
        AtomicBoolean permitted = new AtomicBoolean();
        lastRequests.compute(serverName, (key, previous) -> {
            if (previous == null || now - previous >= minimumNanos) {
                permitted.set(true);
                return now;
            }
            return previous;
        });
        return permitted.get();
    }
}

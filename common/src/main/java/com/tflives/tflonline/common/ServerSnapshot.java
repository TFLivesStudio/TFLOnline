package com.tflives.tflonline.common;

import java.util.Objects;

public record ServerSnapshot(String alias, int players, int capacity, String state) {
    public ServerSnapshot {
        Objects.requireNonNull(alias, "alias");
        Objects.requireNonNull(state, "state");
        if (players < 0 || capacity < 0) {
            throw new IllegalArgumentException("Player count and capacity must be non-negative");
        }
    }
}

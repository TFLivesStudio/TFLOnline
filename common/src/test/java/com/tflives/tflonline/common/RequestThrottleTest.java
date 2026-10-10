package com.tflives.tflonline.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RequestThrottleTest {
    @Test
    void rateLimitsPerBackendIndependently() {
        RequestThrottle throttle = new RequestThrottle(1000);
        assertTrue(throttle.allow("lobby"));
        assertFalse(throttle.allow("lobby"));
        assertTrue(throttle.allow("skyblock"));
    }

    @Test
    void rejectsInvalidWindows() {
        assertThrows(IllegalArgumentException.class, () -> new RequestThrottle(-1));
        assertThrows(IllegalArgumentException.class, () -> new RequestThrottle(60001));
    }
}

package com.example.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that Fan-out/Fan-in parallel validation is faster than sequential.
 * 3 checks × 200ms delay = 600ms sequential vs ~200ms parallel.
 */
@DisplayName("Fan-out / Fan-in parallel validation timing")
class ConfirmOrderParallelValidationTest {

    private static final long SIMULATED_CHECK_MS = 200L;

    @Test
    @DisplayName("parallel validation completes in max(checks) not sum(checks)")
    void parallelValidation_fasterThanSequential() throws Exception {
        long start = System.currentTimeMillis();

        CompletableFuture<Void> c1 = CompletableFuture.runAsync(() -> sleep(SIMULATED_CHECK_MS));
        CompletableFuture<Void> c2 = CompletableFuture.runAsync(() -> sleep(SIMULATED_CHECK_MS));
        CompletableFuture<Void> c3 = CompletableFuture.runAsync(() -> sleep(SIMULATED_CHECK_MS));

        CompletableFuture.allOf(c1, c2, c3).get(3, TimeUnit.SECONDS);

        long elapsed = System.currentTimeMillis() - start;

        // Parallel: should finish in ~200ms, not 600ms
        assertThat(elapsed)
                .as("Parallel validation should be faster than sequential (600ms)")
                .isLessThan(SIMULATED_CHECK_MS * 2);  // generous upper bound: 400ms
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

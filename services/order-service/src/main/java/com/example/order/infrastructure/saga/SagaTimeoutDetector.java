package com.example.order.infrastructure.saga;

import com.example.order.application.saga.OrderSagaOrchestrator;
import com.example.order.application.saga.OrderSagaStep;
import com.example.order.infrastructure.persistence.entity.SagaInstance;
import com.example.order.infrastructure.persistence.repository.JpaSagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Periodic detector for stalled sagas.
 *
 * <p>Poll interval and timeout are configurable via {@link SagaProperties}:
 * <pre>
 * saga:
 *   timeout: 5m
 *   poll-interval: 30s
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaTimeoutDetector {

    private final JpaSagaRepository sagaRepository;
    private final OrderSagaOrchestrator orchestrator;
    private final SagaProperties sagaProperties;

    @Scheduled(fixedDelayString = "#{@sagaProperties.pollInterval.toMillis()}")
    public void detectStalledSagas() {
        List<String> activeSteps = List.of(
                OrderSagaStep.RESERVING_STOCK.name(),
                OrderSagaStep.PROCESSING_PAYMENT.name(),
                OrderSagaStep.COMPENSATING_STOCK.name()
        );

        Instant cutoff = Instant.now().minus(sagaProperties.getTimeout());
        List<SagaInstance> stalled = sagaRepository
                .findByCurrentStepInAndUpdatedAtBeforeAndCompletedAtIsNull(activeSteps, cutoff);

        if (!stalled.isEmpty()) {
            log.warn("Found {} stalled saga(s) (timeout={}), initiating compensation...",
                    stalled.size(), sagaProperties.getTimeout());
        }

        for (SagaInstance saga : stalled) {
            try {
                orchestrator.compensateTimedOutSaga(saga);
            } catch (Exception e) {
                log.error("Failed to compensate stalled saga {}: {}", saga.getId(), e.getMessage(), e);
            }
        }
    }
}

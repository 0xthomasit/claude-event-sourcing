package com.example.order.infrastructure.persistence.repository;

import com.example.order.infrastructure.persistence.entity.SagaInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaSagaRepository extends JpaRepository<SagaInstance, UUID> {

    Optional<SagaInstance> findByOrderId(String orderId);

    /** Find sagas stuck at non-terminal steps older than the given cutoff. */
    List<SagaInstance> findByCurrentStepInAndUpdatedAtBeforeAndCompletedAtIsNull(
            Collection<String> steps, Instant cutoff);
}

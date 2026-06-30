package com.example.order.infrastructure.persistence.repository;

import com.example.order.infrastructure.persistence.entity.EventStoreEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaEventStoreRepository extends JpaRepository<EventStoreEntry, Long> {
    List<EventStoreEntry> findByAggregateIdOrderBySequenceNumberAsc(String aggregateId);
    long countByAggregateId(String aggregateId);
}

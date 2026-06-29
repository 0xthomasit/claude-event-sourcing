package com.example.inventory.infrastructure.persistence.repository;

import com.example.inventory.infrastructure.persistence.entity.EventStoreEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaEventStoreRepository extends JpaRepository<EventStoreEntry, Long> {
    List<EventStoreEntry> findByAggregateIdOrderBySequenceNumberAsc(String aggregateId);
    List<EventStoreEntry> findByAggregateIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
            String aggregateId, long sequenceNumber);
    long countByAggregateId(String aggregateId);
}
package com.example.payment.infrastructure.persistence.repository;

import com.example.payment.infrastructure.persistence.entity.EventStoreEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaEventStoreRepository extends JpaRepository<EventStoreEntry, Long> {

    List<EventStoreEntry> findByAggregateIdOrderBySequenceNumberAsc(String aggregateId);

    @Query("SELECT MAX(e.sequenceNumber) FROM EventStoreEntry e WHERE e.aggregateId = :aggregateId")
    Long findMaxSequenceNumber(@Param("aggregateId") String aggregateId);

    @Query("SELECT COUNT(e) FROM EventStoreEntry e WHERE e.aggregateId = :aggregateId")
    long countByAggregateId(@Param("aggregateId") String aggregateId);
}

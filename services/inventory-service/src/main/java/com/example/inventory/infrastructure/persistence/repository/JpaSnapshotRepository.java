package com.example.inventory.infrastructure.persistence.repository;

import com.example.inventory.infrastructure.persistence.entity.StockSnapshotEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaSnapshotRepository extends JpaRepository<StockSnapshotEntry, String> {
    Optional<StockSnapshotEntry> findById(String aggregateId);
}
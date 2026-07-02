package com.example.promotion.infrastructure.persistence.repository;

import com.example.promotion.domain.model.Promotion;
import com.example.promotion.domain.model.PromotionStatus;
import com.example.promotion.domain.repository.PromotionRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface JpaPromotionRepository extends JpaRepository<Promotion, UUID>, PromotionRepository {

    @Override
    Optional<Promotion> findById(UUID id);

    /**
     * Pessimistic lock for flash sale — prevents concurrent redemption race conditions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promotion p WHERE p.code = :code")
    Optional<Promotion> findByCodeForUpdate(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promotion p WHERE p.id = :id")
    Optional<Promotion> findByIdForUpdate(@Param("id") UUID id);

    Page<Promotion> findByStatusAndStartDateBeforeAndEndDateAfter(
            PromotionStatus status, Instant now1, Instant now2, Pageable pageable);
}

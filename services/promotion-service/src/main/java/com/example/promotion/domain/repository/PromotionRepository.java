package com.example.promotion.domain.repository;

import com.example.promotion.domain.model.Promotion;

import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository {
    Promotion save(Promotion promotion);
    Optional<Promotion> findById(UUID id);
    Optional<Promotion> findByCode(String code);
    boolean existsByCode(String code);
}

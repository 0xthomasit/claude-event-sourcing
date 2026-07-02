package com.example.promotion.domain.repository;

import com.example.promotion.domain.model.PromotionRedemption;
import com.example.promotion.domain.model.RedemptionStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RedemptionRepository {
    PromotionRedemption save(PromotionRedemption redemption);
    Optional<PromotionRedemption> findById(UUID id);
    List<PromotionRedemption> findByOrderId(String orderId);
    List<PromotionRedemption> findByOrderIdAndStatus(String orderId, RedemptionStatus status);
    long countByCustomerIdAndPromotionId(String customerId, UUID promotionId);
}

package com.example.promotion.infrastructure.persistence.repository;

import com.example.promotion.domain.model.PromotionRedemption;
import com.example.promotion.domain.model.RedemptionStatus;
import com.example.promotion.domain.repository.RedemptionRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaRedemptionRepository extends JpaRepository<PromotionRedemption, UUID>, RedemptionRepository {

    @Override
    java.util.Optional<PromotionRedemption> findById(UUID id);

    List<PromotionRedemption> findByOrderId(String orderId);

    List<PromotionRedemption> findByOrderIdAndStatus(String orderId, RedemptionStatus status);

    long countByCustomerIdAndPromotionId(String customerId, UUID promotionId);
}

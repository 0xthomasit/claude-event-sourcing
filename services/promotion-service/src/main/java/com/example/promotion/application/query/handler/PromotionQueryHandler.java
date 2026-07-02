package com.example.promotion.application.query.handler;

import com.example.promotion.application.query.dto.PromotionResponse;
import com.example.promotion.domain.model.PromotionStatus;
import com.example.promotion.infrastructure.persistence.repository.JpaPromotionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromotionQueryHandler {

    private final JpaPromotionRepository promotionRepository;

    @Transactional(readOnly = true)
    public PromotionResponse findById(UUID id) {
        return promotionRepository.findById(id)
                .map(PromotionResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Promotion not found: " + id));
    }

    @Transactional(readOnly = true)
    public PromotionResponse findByCode(String code) {
        return promotionRepository.findByCode(code.toUpperCase())
                .map(PromotionResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Promotion not found: " + code));
    }

    @Transactional(readOnly = true)
    public Page<PromotionResponse> findActive(Pageable pageable) {
        Instant now = Instant.now();
        return promotionRepository
                .findByStatusAndStartDateBeforeAndEndDateAfter(
                        PromotionStatus.ACTIVE, now, now, pageable)
                .map(PromotionResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<PromotionResponse> findAll(Pageable pageable) {
        return promotionRepository.findAll(pageable)
                .map(PromotionResponse::from);
    }
}

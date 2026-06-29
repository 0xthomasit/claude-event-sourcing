package com.example.product.application.service;

import com.example.product.application.dto.*;
import com.example.product.domain.model.Category;
import com.example.product.domain.model.Product;
import com.example.product.domain.model.ProductStatus;
import com.example.product.domain.repository.CategoryRepository;
import com.example.product.domain.repository.ProductRepository;
import com.example.product.infrastructure.messaging.ProductEventPublisher;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository     productRepository;
    private final CategoryRepository    categoryRepository;
    private final ProductEventPublisher eventPublisher;

    // ─── Commands ─────────────────────────────────────────────────────────────

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (productRepository.existsBySku(request.getSku()))
            throw new IllegalArgumentException("SKU already exists: " + request.getSku());

        Category category = resolveCategory(request.getCategoryId());

        Product product = productRepository.save(Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .currency(request.getCurrency())
                .category(category)
                .imageUrl(request.getImageUrl())
                .status(ProductStatus.ACTIVE)
                .build());

        log.info("Product created: {} ({})", product.getSku(), product.getId());
        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        Product product = findProductOrThrow(id);

        boolean priceChanged = false;
        BigDecimal oldPrice  = product.getPrice();

        if (StringUtils.hasText(request.getName()))
            product.setName(request.getName());
        if (request.getDescription() != null)
            product.setDescription(request.getDescription());
        if (request.getPrice() != null && product.isPriceChanged(request.getPrice())) {
            product.updatePrice(request.getPrice());
            priceChanged = true;
        }
        if (request.getCategoryId() != null)
            product.setCategory(resolveCategory(request.getCategoryId()));
        if (request.getImageUrl() != null)
            product.setImageUrl(request.getImageUrl());

        Product saved = productRepository.save(product);

        // Publish price.updated event so Cart/Order services can cache-bust
        if (priceChanged) {
            eventPublisher.publishPriceUpdated(saved, oldPrice);
            log.info("Product price updated: {} {} → {}", saved.getSku(), oldPrice, saved.getPrice());
        }

        return toResponse(saved);
    }

    @Transactional
    public void deactivate(UUID id) {
        Product product = findProductOrThrow(id);
        product.deactivate();
        productRepository.save(product);
        log.info("Product deactivated: {}", id);
    }

    @Transactional
    public void discontinue(UUID id) {
        Product product = findProductOrThrow(id);
        product.discontinue();
        productRepository.save(product);
        log.info("Product discontinued: {}", id);
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        return toResponse(findProductOrThrow(id));
    }

    @Transactional(readOnly = true)
    public ProductResponse findBySku(String sku) {
        return productRepository.findBySku(sku)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with SKU: " + sku));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findByStatus(ProductStatus.ACTIVE, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findByCategory(UUID categoryId, Pageable pageable) {
        return productRepository.findByCategoryIdAndStatus(categoryId, ProductStatus.ACTIVE, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String keyword, Pageable pageable) {
        return productRepository.searchByKeyword(keyword, pageable)
                .map(this::toResponse);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Product findProductOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + categoryId));
    }

    private ProductResponse toResponse(Product p) {
        ProductResponse.CategoryInfo categoryInfo = null;
        if (p.getCategory() != null) {
            categoryInfo = ProductResponse.CategoryInfo.builder()
                    .id(p.getCategory().getId())
                    .name(p.getCategory().getName())
                    .slug(p.getCategory().getSlug())
                    .build();
        }
        return ProductResponse.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .currency(p.getCurrency())
                .status(p.getStatus().name())
                .imageUrl(p.getImageUrl())
                .category(categoryInfo)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
package com.example.review.infrastructure.persistence.repository;

import com.example.review.domain.model.ProductRatingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProductRatingRepository extends JpaRepository<ProductRatingSnapshot, String> {
}

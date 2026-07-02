package com.example.review.domain.model;

public enum ReviewStatus {
    PENDING,    // Submitted, awaiting admin moderation
    APPROVED,   // Approved — visible to other customers
    REJECTED    // Rejected by admin — not visible
}

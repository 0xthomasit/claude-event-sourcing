package com.example.review.interfaces.rest;

import com.example.review.application.command.dto.SubmitReviewCommand;
import com.example.review.application.command.handler.ModerateReviewHandler;
import com.example.review.application.command.handler.SubmitReviewHandler;
import com.example.review.application.query.dto.RatingSnapshotResponse;
import com.example.review.application.query.dto.ReviewResponse;
import com.example.review.application.query.handler.ReviewQueryHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product review and rating management")
public class ReviewController {

    private final SubmitReviewHandler   submitHandler;
    private final ModerateReviewHandler moderateHandler;
    private final ReviewQueryHandler    queryHandler;

    // ─── Customer endpoints ───────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Submit a product review")
    public ResponseEntity<ReviewResponse> submit(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody SubmitReviewCommand cmd) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReviewResponse.from(submitHandler.handle(userId, cmd)));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get approved reviews for a product")
    public ResponseEntity<Page<ReviewResponse>> getByProduct(
            @PathVariable String productId, Pageable pageable) {
        return ResponseEntity.ok(queryHandler.findByProduct(productId, pageable));
    }

    @GetMapping("/product/{productId}/rating")
    @Operation(summary = "Get rating snapshot for a product")
    public ResponseEntity<RatingSnapshotResponse> getRating(@PathVariable String productId) {
        return ResponseEntity.ok(queryHandler.getRatingSnapshot(productId));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get reviews by customer")
    public ResponseEntity<Page<ReviewResponse>> getByCustomer(
            @PathVariable String customerId, Pageable pageable) {
        return ResponseEntity.ok(queryHandler.findByCustomer(customerId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get review by ID")
    public ResponseEntity<ReviewResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(queryHandler.findById(id));
    }

    // ─── Admin endpoints ──────────────────────────────────────────────────────

    @GetMapping("/admin/pending")
    @Operation(summary = "List pending reviews (ADMIN)")
    public ResponseEntity<Page<ReviewResponse>> listPending(Pageable pageable) {
        return ResponseEntity.ok(queryHandler.findPending(pageable));
    }

    @PostMapping("/admin/{id}/approve")
    @Operation(summary = "Approve a review (ADMIN)")
    public ResponseEntity<ReviewResponse> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ReviewResponse.from(moderateHandler.approve(id)));
    }

    @PostMapping("/admin/{id}/reject")
    @Operation(summary = "Reject a review (ADMIN)")
    public ResponseEntity<ReviewResponse> reject(
            @PathVariable UUID id, @RequestParam String reason) {
        return ResponseEntity.ok(ReviewResponse.from(moderateHandler.reject(id, reason)));
    }
}

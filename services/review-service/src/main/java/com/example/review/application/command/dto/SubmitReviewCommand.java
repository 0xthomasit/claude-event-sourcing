package com.example.review.application.command.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubmitReviewCommand {
    @NotBlank private String productId;
    @NotBlank private String orderId;
    @Min(1) @Max(5) private int rating;
    private String title;
    @NotBlank @Size(min = 10, max = 2000) private String content;
    private List<String> imageUrls;
}

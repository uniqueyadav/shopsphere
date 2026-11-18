package com.shopsphere.review.dto;

import java.time.Instant;
import com.shopsphere.common.enums.ReviewStatus;

public record ReviewResponse(Long id, Long productId, String productName, String productSku, String productImageUrl, String categoryName, String storeName, Long customerId, String customerName, String customerEmail, int rating, String reviewText, ReviewStatus status, String adminNote, String rejectionReason, Instant createdAt, Instant moderatedAt, long productReviewCount, double productAverageRating) {}

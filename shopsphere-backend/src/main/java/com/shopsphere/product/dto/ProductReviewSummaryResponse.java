package com.shopsphere.product.dto;

import java.util.Map;

public record ProductReviewSummaryResponse(double averageRating, long reviewCount, Map<Integer, Long> ratingCounts) {}

package com.shopsphere.review.dto;

import com.shopsphere.common.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(@NotNull ReviewStatus status, @Size(max=1000) String adminNote, @Size(max=500) String rejectionReason) {}

package com.shopsphere.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerSuspensionRequest(@NotBlank @Size(max = 1000) String reason) {
}

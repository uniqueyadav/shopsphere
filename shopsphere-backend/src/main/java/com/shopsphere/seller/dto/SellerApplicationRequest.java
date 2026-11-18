package com.shopsphere.seller.dto;

import com.shopsphere.common.enums.BusinessType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SellerApplicationRequest(
		@NotBlank @Size(max = 150) String businessName,
		@NotBlank @Size(max = 100) String sellerFullName,
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(max = 20) @Pattern(regexp = "^[+0-9().\\-\\s]{7,20}$") String phone,
		@NotBlank @Size(max = 1000) String businessAddress,
		@Size(max = 15) @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$") String gstNumber,
		@NotNull BusinessType businessType,
		@Size(max = 2000) String businessDescription) {
}
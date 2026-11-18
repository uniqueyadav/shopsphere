package com.shopsphere.product.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductRequest(
		@NotBlank(message = "Product name is required.")
		@Size(max = 150, message = "Product name must be 150 characters or fewer.")
		String productName,
		@Size(max = 180, message = "Product slug must be 180 characters or fewer.")
		@Pattern(
				regexp = "(?iu)^(?:|[\\p{L}\\p{N}]+(?:-[\\p{L}\\p{N}]+)*)$",
				message = "Product slug may contain only letters, numbers, and hyphens.")
		String slug,
		@Size(max = 5000, message = "Product description must be 5,000 characters or fewer.")
		String description,
		@Size(max = 100, message = "Brand must be 100 characters or fewer.")
		String brand,
		@NotBlank(message = "SKU is required.")
		@Size(max = 100, message = "SKU must be 100 characters or fewer.")
		@Pattern(regexp = "^[A-Za-z0-9._/-]+$", message = "SKU contains unsupported characters.")
		String sku,
		@NotNull(message = "Category is required.")
		@Positive(message = "Category ID must be positive.")
		Long categoryId,
		@NotNull(message = "Price is required.")
		@DecimalMin(value = "0.01", message = "Price must be greater than zero.")
		@Digits(integer = 10, fraction = 2, message = "Price supports up to 10 digits and 2 decimals.")
		BigDecimal price,
		@DecimalMin(value = "0.01", message = "Discount price must be greater than zero.")
		@Digits(integer = 10, fraction = 2, message = "Discount price supports up to 10 digits and 2 decimals.")
		BigDecimal discountPrice,
		@NotNull(message = "Stock quantity is required.")
		@Min(value = 0, message = "Stock quantity cannot be negative.")
		Integer stockQuantity) {
}
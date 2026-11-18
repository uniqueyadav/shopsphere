package com.shopsphere.product.controller;

import java.math.BigDecimal;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.product.dto.ProductPageResponse;
import com.shopsphere.product.dto.ProductFacetsResponse;
import com.shopsphere.product.dto.ProductReviewSummaryResponse;
import com.shopsphere.product.dto.ProductResponse;
import com.shopsphere.product.service.ProductService;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

	private final ProductService service;

	public ProductController(ProductService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse<ProductPageResponse> listProducts(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(required = false) @Size(max = 200) String search,
			@RequestParam(required = false) @Positive Long categoryId,
			@RequestParam(required = false) @Size(max = 100) String brand,
			@RequestParam(required = false) @DecimalMin(value = "0", message = "Minimum price cannot be negative.")
			BigDecimal minPrice,
			@RequestParam(required = false) @DecimalMin(value = "0", message = "Maximum price cannot be negative.")
			BigDecimal maxPrice,
			@RequestParam(defaultValue = "false") boolean discountedOnly,
			@RequestParam(defaultValue = "ALL")
			@Pattern(regexp = "(?i)ALL|IN_STOCK|OUT_OF_STOCK", message = "Availability filter is invalid.")
			String availability,
			@RequestParam(defaultValue = "newest")
			@Pattern(regexp = "newest|price_asc|price_desc|popular", message = "Sort option is invalid.")
			String sort,
			@RequestParam(required = false) @Min(1) @Max(5) Integer minimumRating) {
		return ApiResponse.success(
				"Approved products retrieved.",
				service.listPublicProducts(
					page, size, search, categoryId, brand, minPrice, maxPrice, discountedOnly, availability, sort, minimumRating));
	}

	@GetMapping("/slug/{slug}")
	public ApiResponse<ProductResponse> getProductBySlug(@PathVariable @Size(max = 200) String slug) {
		return ApiResponse.success("Product retrieved.", service.getPublicProductBySlug(slug));
	}

	@GetMapping("/facets")
	public ApiResponse<ProductFacetsResponse> getProductFacets() {
		return ApiResponse.success("Product filters retrieved.", service.getPublicProductFacets());
	}

	@GetMapping("/{id}/review-summary")
	public ApiResponse<ProductReviewSummaryResponse> getReviewSummary(@PathVariable @Positive Long id) {
		return ApiResponse.success("Approved product reviews summarized.", service.getPublicReviewSummary(id));
	}

	@GetMapping("/{id}")
	public ApiResponse<ProductResponse> getProduct(@PathVariable @Positive Long id) {
		return ApiResponse.success("Product retrieved.", service.getPublicProduct(id));
	}
}

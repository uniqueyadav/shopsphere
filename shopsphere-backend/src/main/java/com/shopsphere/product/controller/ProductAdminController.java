package com.shopsphere.product.controller;

import com.shopsphere.common.enums.ProductStatus;
import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.product.dto.ProductPageResponse;
import com.shopsphere.product.dto.ProductRejectionRequest;
import com.shopsphere.product.dto.ProductResponse;
import com.shopsphere.product.dto.ProductRequest;
import com.shopsphere.product.dto.AdminProductCreateRequest;
import com.shopsphere.product.service.ProductService;
import com.shopsphere.security.CustomUserDetails;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/v1/admin/products")
public class ProductAdminController {

	private final ProductService service;

	public ProductAdminController(ProductService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse<ProductPageResponse> listProducts(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(required = false) @Size(max = 200) String search,
			@RequestParam(required = false) ProductStatus status,
			@RequestParam(required = false) @Positive Long categoryId,
			@RequestParam(required = false) @Positive Long sellerId,
			@RequestParam(required = false) Boolean active) {
		return ApiResponse.success(
				"Products retrieved.",
				service.listAdminProducts(page, size, search, status, categoryId, sellerId, active));
	}

	@GetMapping("/stats")
	public ApiResponse<?> stats() {
		return ApiResponse.success("Product statistics retrieved.", service.getAdminProductStats());
	}

	@GetMapping("/sellers")
	public ApiResponse<?> sellers() {
		return ApiResponse.success("Active approved sellers retrieved.", service.getAdminSellerOptions());
	}

	@GetMapping(value = "/export", produces = "text/csv")
	public ResponseEntity<String> export(
			@RequestParam(required = false) @Size(max = 200) String search,
			@RequestParam(required = false) ProductStatus status,
			@RequestParam(required = false) @Positive Long categoryId,
			@RequestParam(required = false) @Positive Long sellerId,
			@RequestParam(required = false) Boolean active) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shopsphere-products.csv")
				.contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
				.body(service.exportAdminProducts(search, status, categoryId, sellerId, active));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ProductResponse> create(@Valid @RequestBody AdminProductCreateRequest request) {
		return ApiResponse.success("Product created as a draft.",
				service.createAdminProduct(request.sellerId(), request.product()));
	}

	@GetMapping("/{id}")
	public ApiResponse<ProductResponse> getProduct(@PathVariable @Positive Long id) {
		return ApiResponse.success("Product retrieved.", service.getAdminProduct(id));
	}

	@org.springframework.web.bind.annotation.PutMapping("/{id}")
	public ApiResponse<ProductResponse> update(
			@PathVariable @Positive Long id,
			@Valid @RequestBody ProductRequest request) {
		return ApiResponse.success("Product updated. It must be reviewed again if it was previously submitted.",
				service.updateAdminProduct(id, request));
	}

	@PostMapping("/{id}/active")
	public ApiResponse<ProductResponse> setActive(
			@PathVariable @Positive Long id,
			@RequestParam boolean value) {
		return ApiResponse.success(value ? "Product activated." : "Product archived.",
				service.setAdminProductActive(id, value));
	}

	@PostMapping("/{id}/approve")
	public ApiResponse<ProductResponse> approveProduct(
			@PathVariable @Positive Long id,
			@AuthenticationPrincipal CustomUserDetails reviewer) {
		return ApiResponse.success("Product approved.", service.approve(id, requireReviewer(reviewer)));
	}

	@PostMapping("/{id}/reject")
	public ApiResponse<ProductResponse> rejectProduct(
			@PathVariable @Positive Long id,
			@Valid @RequestBody ProductRejectionRequest request,
			@AuthenticationPrincipal CustomUserDetails reviewer) {
		return ApiResponse.success(
				"Product rejected.",
				service.reject(id, requireReviewer(reviewer), request.rejectionReason()));
	}

	private Long requireReviewer(CustomUserDetails reviewer) {
		if (reviewer == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
		}
		return reviewer.getId();
	}
}

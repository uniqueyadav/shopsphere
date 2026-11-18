package com.shopsphere.product.controller;

import com.shopsphere.common.enums.ProductStatus;
import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.product.dto.ProductPageResponse;
import com.shopsphere.product.dto.ProductRequest;
import com.shopsphere.product.dto.ProductResponse;
import com.shopsphere.product.service.ProductService;
import com.shopsphere.security.CustomUserDetails;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/v1/seller/products")
public class SellerProductController {

	private final ProductService service;

	public SellerProductController(ProductService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse<ProductPageResponse> listProducts(
			@AuthenticationPrincipal CustomUserDetails seller,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(required = false) @Size(max = 200) String search,
			@RequestParam(required = false) ProductStatus status,
			@RequestParam(required = false) @Positive Long categoryId,
			@RequestParam(required = false) Boolean active,
			@RequestParam(required = false) Boolean outOfStock,
			@RequestParam(defaultValue = "newest") @Pattern(regexp = "newest|name_asc|name_desc|price_asc|price_desc|stock_asc|stock_desc") String sort) {
		return ApiResponse.success(
				"Seller products retrieved.",
				service.listSellerProducts(requireSeller(seller), page, size, search, status, categoryId, active, outOfStock, sort));
	}

	@GetMapping("/{id}")
	public ApiResponse<ProductResponse> getProduct(
			@AuthenticationPrincipal CustomUserDetails seller,
			@PathVariable @Positive Long id) {
		return ApiResponse.success("Seller product retrieved.", service.getSellerProduct(requireSeller(seller), id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ProductResponse> createProduct(
			@AuthenticationPrincipal CustomUserDetails seller,
			@Valid @RequestBody ProductRequest request) {
		return ApiResponse.success("Product created as a draft.", service.create(requireSeller(seller), request));
	}

	@PutMapping("/{id}")
	public ApiResponse<ProductResponse> updateProduct(
			@AuthenticationPrincipal CustomUserDetails seller,
			@PathVariable @Positive Long id,
			@Valid @RequestBody ProductRequest request) {
		return ApiResponse.success("Product updated.", service.update(requireSeller(seller), id, request));
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> deleteProduct(
			@AuthenticationPrincipal CustomUserDetails seller,
			@PathVariable @Positive Long id) {
		service.delete(requireSeller(seller), id);
		return ApiResponse.success("Product permanently deleted.", null);
	}

	@PostMapping("/{id}/submit-for-approval")
	public ApiResponse<ProductResponse> submitForApproval(
			@AuthenticationPrincipal CustomUserDetails seller,
			@PathVariable @Positive Long id) {
		return ApiResponse.success(
				"Product submitted for approval.",
				service.submitForApproval(requireSeller(seller), id));
	}

	private Long requireSeller(CustomUserDetails seller) {
		if (seller == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
		}
		return seller.getId();
	}
}

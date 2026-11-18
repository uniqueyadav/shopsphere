package com.shopsphere.product.controller;

import java.util.List;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.product.dto.ProductImageOrderRequest;
import com.shopsphere.product.dto.ProductImageResponse;
import com.shopsphere.product.service.ProductImageService;
import com.shopsphere.security.CustomUserDetails;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class ProductImageController {

	private final ProductImageService service;

	public ProductImageController(ProductImageService service) {
		this.service = service;
	}

	@GetMapping("/seller/products/{productId}/images")
	public ApiResponse<List<ProductImageResponse>> listSellerImages(
			@AuthenticationPrincipal CustomUserDetails seller,
			@PathVariable @Positive Long productId) {
		return ApiResponse.success(
				"Product images retrieved.",
				service.listForSeller(requireSeller(seller), productId));
	}

	@PostMapping("/seller/products/{productId}/images")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<List<ProductImageResponse>> uploadImages(
			@AuthenticationPrincipal CustomUserDetails seller,
			@PathVariable @Positive Long productId,
			@RequestParam("files") List<MultipartFile> files) {
		return ApiResponse.success(
				"Product images uploaded.",
				service.upload(requireSeller(seller), productId, files));
	}

	@PutMapping("/seller/products/{productId}/images/{imageId}/primary")
	public ApiResponse<List<ProductImageResponse>> setPrimaryImage(
			@AuthenticationPrincipal CustomUserDetails seller,
			@PathVariable @Positive Long productId,
			@PathVariable @Positive Long imageId) {
		return ApiResponse.success(
				"Primary product image updated.",
				service.setPrimary(requireSeller(seller), productId, imageId));
	}

	@PutMapping("/seller/products/{productId}/images/reorder")
	public ApiResponse<List<ProductImageResponse>> reorderImages(
			@AuthenticationPrincipal CustomUserDetails seller,
			@PathVariable @Positive Long productId,
			@Valid @RequestBody ProductImageOrderRequest request) {
		return ApiResponse.success(
				"Product images reordered.",
				service.reorder(requireSeller(seller), productId, request.imageIds()));
	}

	@DeleteMapping("/seller/products/{productId}/images/{imageId}")
	public ApiResponse<List<ProductImageResponse>> deleteImage(
			@AuthenticationPrincipal CustomUserDetails seller,
			@PathVariable @Positive Long productId,
			@PathVariable @Positive Long imageId) {
		return ApiResponse.success(
				"Product image removed.",
				service.delete(requireSeller(seller), productId, imageId));
	}

	@GetMapping("/admin/products/{productId}/images")
	public ApiResponse<List<ProductImageResponse>> listAdminImages(@PathVariable @Positive Long productId) {
		return ApiResponse.success("Product images retrieved.", service.listForAdmin(productId));
	}

	@PostMapping("/admin/products/{productId}/images")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<List<ProductImageResponse>> uploadAdminImages(
			@PathVariable @Positive Long productId,
			@RequestParam("files") List<MultipartFile> files) {
		return ApiResponse.success("Product images uploaded.", service.uploadForAdmin(productId, files));
	}

	@PutMapping("/admin/products/{productId}/images/{imageId}/primary")
	public ApiResponse<List<ProductImageResponse>> setAdminPrimaryImage(
			@PathVariable @Positive Long productId,
			@PathVariable @Positive Long imageId) {
		return ApiResponse.success("Primary product image updated.", service.setPrimaryForAdmin(productId, imageId));
	}

	@PutMapping("/admin/products/{productId}/images/reorder")
	public ApiResponse<List<ProductImageResponse>> reorderAdminImages(
			@PathVariable @Positive Long productId,
			@Valid @RequestBody ProductImageOrderRequest request) {
		return ApiResponse.success("Product images reordered.", service.reorderForAdmin(productId, request.imageIds()));
	}

	@DeleteMapping("/admin/products/{productId}/images/{imageId}")
	public ApiResponse<List<ProductImageResponse>> deleteAdminImage(
			@PathVariable @Positive Long productId,
			@PathVariable @Positive Long imageId) {
		return ApiResponse.success("Product image removed.", service.deleteForAdmin(productId, imageId));
	}

	@GetMapping("/products/{productId}/images")
	public ApiResponse<List<ProductImageResponse>> listPublicImages(@PathVariable @Positive Long productId) {
		return ApiResponse.success("Product images retrieved.", service.listPublic(productId));
	}

	private Long requireSeller(CustomUserDetails seller) {
		if (seller == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
		}
		return seller.getId();
	}
}

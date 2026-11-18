package com.shopsphere.category.controller;

import com.shopsphere.category.dto.CategoryPageResponse;
import com.shopsphere.category.dto.CategoryRequest;
import com.shopsphere.category.dto.CategoryResponse;
import com.shopsphere.category.dto.CategoryStatsResponse;
import com.shopsphere.category.dto.CategoryImageUploadResponse;
import com.shopsphere.category.service.CategoryService;
import com.shopsphere.category.service.CategoryImageService;
import com.shopsphere.common.response.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class CategoryController {

	private final CategoryService service;
	private final CategoryImageService imageService;

	public CategoryController(CategoryService service, CategoryImageService imageService) {
		this.service = service;
		this.imageService = imageService;
	}

	@GetMapping("/categories")
	public ApiResponse<CategoryPageResponse> listActiveCategories(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(required = false) @Size(max = 100) String search) {
		return ApiResponse.success("Active categories retrieved.", service.listActive(page, size, search));
	}

	@GetMapping("/categories/{id}")
	public ApiResponse<CategoryResponse> getActiveCategory(@PathVariable @Positive Long id) {
		return ApiResponse.success("Category retrieved.", service.getActiveById(id));
	}

	@GetMapping("/admin/categories")
	public ApiResponse<CategoryPageResponse> listCategories(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(required = false) @Size(max = 100) String search,
			@RequestParam(required = false) Boolean active,
			@RequestParam(required = false) @Min(0) Long parentId,
			@RequestParam(defaultValue = "name_asc") String sort) {
		return ApiResponse.success("Categories retrieved.", service.listAll(page, size, search, active, parentId, sort));
	}

	@GetMapping("/admin/categories/stats")
	public ApiResponse<CategoryStatsResponse> categoryStatistics() {
		return ApiResponse.success("Category statistics retrieved.", service.statistics());
	}

	@GetMapping("/admin/categories/parents")
	public ApiResponse<java.util.List<CategoryResponse>> parentCategories() {
		return ApiResponse.success("Parent categories retrieved.", service.parentCategories());
	}

	@PostMapping(value = "/admin/categories/images", consumes = "multipart/form-data")
	public ApiResponse<CategoryImageUploadResponse> uploadCategoryImage(@RequestParam("image") MultipartFile image) {
		return ApiResponse.success("Category image uploaded.", imageService.upload(image));
	}

	@DeleteMapping("/admin/categories/images")
	public ApiResponse<Void> deleteUnusedCategoryImage(@RequestParam String imagePublicId) {
		imageService.deleteIfUnreferenced(imagePublicId);
		return ApiResponse.success("Unreferenced category image removed.", null);
	}

	@GetMapping("/admin/categories/{id}")
	public ApiResponse<CategoryResponse> getCategory(@PathVariable @Positive Long id) {
		return ApiResponse.success("Category retrieved.", service.getById(id));
	}

	@PostMapping("/admin/categories")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
		return ApiResponse.success("Category created.", service.create(request));
	}

	@PutMapping("/admin/categories/{id}")
	public ApiResponse<CategoryResponse> updateCategory(
			@PathVariable @Positive Long id,
			@Valid @RequestBody CategoryRequest request) {
		return ApiResponse.success("Category updated.", service.update(id, request));
	}

	@DeleteMapping("/admin/categories/{id}")
	public ApiResponse<CategoryResponse> deleteCategory(@PathVariable @Positive Long id) {
		return ApiResponse.success("Category deactivated.", service.delete(id));
	}
}

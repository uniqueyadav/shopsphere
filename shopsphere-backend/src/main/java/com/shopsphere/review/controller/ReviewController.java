package com.shopsphere.review.controller;

import com.shopsphere.common.enums.ReviewStatus;
import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.review.dto.ReviewRequest;
import com.shopsphere.review.service.ReviewService;
import com.shopsphere.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class ReviewController {
	private final ReviewService service;
	public ReviewController(ReviewService service){this.service=service;}
	@GetMapping public ApiResponse<Page<?>> list(@RequestParam(required=false) String search,@RequestParam(required=false) Integer rating,@RequestParam(required=false) ReviewStatus status,@RequestParam(required=false) Long productId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size){return ApiResponse.success("Reviews loaded.",service.list(search,rating,status,productId,page,size));}
	@GetMapping("/stats") public ApiResponse<?> stats(){return ApiResponse.success("Review statistics loaded.",service.stats());}
	@GetMapping("/{id}") public ApiResponse<?> get(@PathVariable Long id){return ApiResponse.success("Review loaded.",service.get(id));}
	@PatchMapping("/{id}/moderation") public ApiResponse<?> moderate(@PathVariable Long id,@Valid @RequestBody ReviewRequest request,@AuthenticationPrincipal CustomUserDetails admin){return ApiResponse.success("Review moderated.",service.moderate(id,request,admin.getId()));}
	@DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable Long id){service.delete(id);return ApiResponse.success("Review deleted.",null);}
}

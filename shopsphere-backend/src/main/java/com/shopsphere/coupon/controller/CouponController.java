package com.shopsphere.coupon.controller;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.coupon.dto.CouponAnalyticsResponse;
import com.shopsphere.coupon.dto.CouponPageResponse;
import com.shopsphere.coupon.dto.CouponRequest;
import com.shopsphere.coupon.dto.CouponResponse;
import com.shopsphere.coupon.dto.CouponStatsResponse;
import com.shopsphere.coupon.dto.CouponUsageResponse;
import com.shopsphere.coupon.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/coupons")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class CouponController {
	private final CouponService coupons;
	public CouponController(CouponService coupons){this.coupons=coupons;}
	@GetMapping public ApiResponse<CouponPageResponse> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,@RequestParam(required=false) String search,@RequestParam(required=false) String status,@RequestParam(required=false) String discountType,@RequestParam(required=false) Long categoryId){return ApiResponse.success("Coupons loaded.",coupons.list(page,size,search,status,discountType,categoryId));}
	@GetMapping("/stats") public ApiResponse<CouponStatsResponse> stats(){return ApiResponse.success("Coupon statistics loaded.",coupons.stats());}
	@GetMapping("/code-availability") public ApiResponse<Boolean> codeAvailability(@RequestParam String code,@RequestParam(required=false) Long excludeId){return ApiResponse.success("Coupon code availability checked.",coupons.codeAvailable(code,excludeId));}
	@GetMapping("/export") public ResponseEntity<String> export(@RequestParam(required=false) String search,@RequestParam(required=false) String status,@RequestParam(required=false) String discountType,@RequestParam(required=false) Long categoryId){StringBuilder csv=new StringBuilder("Code,Name,Discount Type,Discount Value,Minimum Order,Usage Count,Usage Limit,Status,Starts At,Expires At\n");for(CouponResponse c:coupons.export(search,status,discountType,categoryId))csv.append(csv(c.code())).append(',').append(csv(c.name())).append(',').append(csv(c.discountType())).append(',').append(csv(c.discountValue())).append(',').append(csv(c.minimumOrderAmount())).append(',').append(csv(c.usageCount())).append(',').append(csv(c.usageLimit())).append(',').append(csv(c.status())).append(',').append(csv(c.startsAt())).append(',').append(csv(c.expiresAt())).append('\n');return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=shopsphere-coupons.csv").contentType(MediaType.parseMediaType("text/csv; charset=UTF-8")).body(csv.toString());}
	@GetMapping("/{couponId}") public ApiResponse<CouponResponse> get(@PathVariable Long couponId){return ApiResponse.success("Coupon details loaded.",coupons.get(couponId));}
	@PostMapping public ApiResponse<CouponResponse> create(@Valid @RequestBody CouponRequest request){return ApiResponse.success("Coupon created.",coupons.create(request));}
	@PutMapping("/{couponId}") public ApiResponse<CouponResponse> update(@PathVariable Long couponId,@Valid @RequestBody CouponRequest request){return ApiResponse.success("Coupon updated.",coupons.update(couponId,request));}
	@PatchMapping("/{couponId}/active") public ApiResponse<CouponResponse> active(@PathVariable Long couponId,@RequestParam boolean value){return ApiResponse.success(value?"Coupon activated.":"Coupon deactivated.",coupons.setActive(couponId,value));}
	@DeleteMapping("/{couponId}") public ApiResponse<Void> archive(@PathVariable Long couponId){coupons.archive(couponId);return ApiResponse.success("Coupon archived.",null);}
	@GetMapping("/{couponId}/usage") public ApiResponse<Page<CouponUsageResponse>> usage(@PathVariable Long couponId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size){return ApiResponse.success("Coupon usage history loaded.",coupons.usageHistory(couponId,page,size));}
	@GetMapping("/{couponId}/analytics") public ApiResponse<CouponAnalyticsResponse> analytics(@PathVariable Long couponId){return ApiResponse.success("Coupon analytics loaded.",coupons.analytics(couponId));}
	private String csv(Object v){String s=v==null?"":v.toString();if(!s.isEmpty()&&"=+-@".indexOf(s.charAt(0))>=0)s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
}

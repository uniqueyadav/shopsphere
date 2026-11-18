package com.shopsphere.coupon.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.shopsphere.category.repository.CategoryRepository;
import com.shopsphere.common.enums.CouponDiscountType;
import com.shopsphere.common.enums.CouponEligibility;
import com.shopsphere.common.enums.CouponScope;
import com.shopsphere.common.enums.Role;
import com.shopsphere.coupon.dto.CouponAnalyticsResponse;
import com.shopsphere.coupon.dto.CouponPageResponse;
import com.shopsphere.coupon.dto.CouponRequest;
import com.shopsphere.coupon.dto.CouponResponse;
import com.shopsphere.coupon.dto.CouponStatsResponse;
import com.shopsphere.coupon.dto.CouponUsageResponse;
import com.shopsphere.coupon.entity.Coupon;
import com.shopsphere.coupon.entity.CouponUsage;
import com.shopsphere.coupon.repository.CouponRepository;
import com.shopsphere.coupon.repository.CouponUsageRepository;
import com.shopsphere.order.entity.Order;
import com.shopsphere.order.repository.OrderRepository;
import com.shopsphere.product.repository.ProductRepository;
import com.shopsphere.user.entity.User;
import com.shopsphere.user.ShopUserRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CouponService {
	private static final BigDecimal ZERO = new BigDecimal("0.00");
	private final CouponRepository coupons;
	private final CouponUsageRepository usages;
	private final ProductRepository products;
	private final CategoryRepository categories;
	private final OrderRepository orders;
	private final ShopUserRepository users;
	public CouponService(CouponRepository coupons,CouponUsageRepository usages,ProductRepository products,CategoryRepository categories,OrderRepository orders,ShopUserRepository users){this.coupons=coupons;this.usages=usages;this.products=products;this.categories=categories;this.orders=orders;this.users=users;}

	@Transactional(readOnly=true)
	public CouponPageResponse list(int page,int size,String search,String status,String discountType,Long categoryId){
		validatePage(page,size);Specification<Coupon> spec=(r,q,cb)->cb.isFalse(r.get("archived"));
		if(search!=null&&!search.isBlank())spec=spec.and((r,q,cb)->{String t="%"+search.trim().toUpperCase(Locale.ROOT)+"%";return cb.or(cb.like(cb.upper(r.get("code")),t),cb.like(cb.upper(r.get("name")),t));});
		if(discountType!=null&&!discountType.isBlank()){CouponDiscountType type=parseType(discountType);spec=spec.and((r,q,cb)->cb.equal(r.get("discountType"),type));}
		if(categoryId!=null)spec=spec.and((r,q,cb)->{q.distinct(true);return cb.equal(r.join("categoryIds",JoinType.INNER),categoryId);});
		if(status!=null&&!status.isBlank())spec=spec.and(statusSpec(status));
		Page<Coupon> result=coupons.findAll(spec,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt")));
		return new CouponPageResponse(result.getContent().stream().map(this::response).toList(),page,size,result.getTotalElements(),result.getTotalPages());
	}
	@Transactional(readOnly=true) public CouponResponse get(Long id){return response(findCoupon(id));}
	@Transactional(readOnly=true) public boolean codeAvailable(String code,Long excludeId){String normalized=normalize(code);return coupons.findByCodeIgnoreCase(normalized).map(c->c.getId().equals(excludeId)).orElse(true);}
	@Transactional(readOnly=true) public CouponStatsResponse stats(){Instant now=Instant.now();return new CouponStatsResponse(coupons.countByArchivedFalse(),coupons.countByArchivedFalseAndActiveTrueAndStartsAtLessThanEqualAndExpiresAtGreaterThan(now,now),coupons.countByArchivedFalseAndExpiresAtLessThanEqual(now),coupons.countInactiveNotExpired(now),coupons.countByArchivedFalseAndActiveTrueAndStartsAtAfter(now));}
	@Transactional public CouponResponse create(CouponRequest request){validateRequest(request);String code=normalize(request.code());if(coupons.existsByCodeIgnoreCase(code))throw conflict("Coupon code already exists.");return response(coupons.save(entity(request,code)));}
	@Transactional public CouponResponse update(Long id,CouponRequest request){Coupon c=coupons.findByIdForUpdate(id).filter(x->!x.isArchived()).orElseThrow(this::notFound);validateRequest(request);String code=normalize(request.code());if(!c.getCode().equals(code)&&coupons.existsByCodeIgnoreCase(code))throw conflict("Coupon code already exists.");if(c.getUsageCount()>0&&(!c.getCode().equals(code)||c.getDiscountType()!=request.discountType()||c.getDiscountValue().compareTo(request.discountValue())!=0))throw conflict("Code and discount rules cannot change after use.");if(request.usageLimit()!=null&&request.usageLimit()<c.getUsageCount())throw conflict("Usage limit cannot be lower than completed uses.");c.update(code,request.name().trim(),trim(request.description()),trim(request.adminNote()),request.discountType(),request.discountValue(),request.minimumOrderAmount(),request.maximumDiscountAmount(),request.usageLimit(),request.perCustomerLimit(),request.startsAt(),request.expiresAt(),request.active(),request.eligibility(),request.scope(),safe(request.productIds()),safe(request.categoryIds()),safe(request.customerIds()));return response(c);}
	@Transactional public CouponResponse setActive(Long id,boolean active){Coupon c=coupons.findByIdForUpdate(id).filter(x->!x.isArchived()).orElseThrow(this::notFound);if(active){Instant now=Instant.now();if(!c.getExpiresAt().isAfter(now))throw conflict("Expired coupons cannot be activated.");if(c.getUsageLimit()!=null&&c.getUsageCount()>=c.getUsageLimit())throw conflict("Coupon usage limit is exhausted.");}c.setActive(active);return response(c);}
	@Transactional public void archive(Long id){Coupon c=coupons.findByIdForUpdate(id).filter(x->!x.isArchived()).orElseThrow(this::notFound);c.archive();}
	@Transactional(readOnly=true) public Page<CouponUsageResponse> usageHistory(Long id,int page,int size){validatePage(page,size);findCoupon(id);return usages.findByCoupon_IdOrderByCreatedAtDesc(id,PageRequest.of(page,size)).map(u->new CouponUsageResponse(u.getId(),u.getCustomer().getFullName(),u.getCustomer().getEmail(),u.getOrder().getId(),u.getDiscountAmount(),u.getOrder().getOrderStatus().name(),u.getCreatedAt()));}
	@Transactional(readOnly=true) public CouponAnalyticsResponse analytics(Long id){findCoupon(id);long count=usages.countByCoupon_Id(id);return new CouponAnalyticsResponse(count,usages.sumDiscountByCouponId(id),count);}
	@Transactional(readOnly=true) public List<CouponResponse> export(String search,String status,String discountType,Long categoryId){CouponPageResponse first=list(0,100,search,status,discountType,categoryId);List<CouponResponse> exported=new ArrayList<>(first.items());for(int page=1;page<first.totalPages();page++)exported.addAll(list(page,100,search,status,discountType,categoryId).items());return exported;}

	@Transactional public CouponQuote quote(String rawCode,Long customerId,List<CouponLine> lines,BigDecimal orderSubtotal){if(rawCode==null||rawCode.isBlank())return null;Coupon c=coupons.findByCodeForUpdate(normalize(rawCode)).orElseThrow(()->unprocessable("Coupon code is invalid."));Instant now=Instant.now();if(c.isArchived()||!c.isActive()||now.isBefore(c.getStartsAt())||!now.isBefore(c.getExpiresAt()))throw unprocessable("Coupon is not currently active.");if(c.getUsageLimit()!=null&&c.getUsageCount()>=c.getUsageLimit())throw conflict("Coupon usage limit has been reached.");if(usages.countByCoupon_IdAndCustomer_Id(c.getId(),customerId)>=c.getPerCustomerLimit())throw conflict("You have reached this coupon's usage limit.");if(c.getEligibility()==CouponEligibility.NEW_CUSTOMERS&&orders.countByCustomer_Id(customerId)>0)throw unprocessable("This coupon is available to new customers only.");if(c.getEligibility()==CouponEligibility.SELECTED_CUSTOMERS&&!c.getCustomerIds().contains(customerId))throw unprocessable("You are not eligible for this coupon.");List<CouponLine> eligible=lines.stream().filter(l->switch(c.getScope()){case ALL_PRODUCTS->true;case SELECTED_PRODUCTS->c.getProductIds().contains(l.productId());case SELECTED_CATEGORIES->c.getCategoryIds().contains(l.categoryId());}).toList();if(eligible.isEmpty())throw unprocessable("Coupon does not apply to products in your cart.");BigDecimal eligibleSubtotal=eligible.stream().map(CouponLine::subtotal).reduce(ZERO,BigDecimal::add);if(eligibleSubtotal.compareTo(c.getMinimumOrderAmount())<0)throw unprocessable("Order does not meet the coupon minimum amount.");BigDecimal discount=c.getDiscountType()==CouponDiscountType.PERCENTAGE?eligibleSubtotal.multiply(c.getDiscountValue()).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP):c.getDiscountValue().min(eligibleSubtotal);if(c.getMaximumDiscountAmount()!=null)discount=discount.min(c.getMaximumDiscountAmount());return new CouponQuote(c,discount.min(orderSubtotal));}
	@Transactional public void recordUsage(CouponQuote quote,Order order,User customer){if(quote!=null){quote.coupon().recordUse();usages.save(new CouponUsage(quote.coupon(),order,customer,quote.discount()));}}

	private Specification<Coupon> statusSpec(String value){Instant n=Instant.now();return switch(value.toUpperCase(Locale.ROOT)){case "ACTIVE"->(r,q,cb)->cb.and(cb.isTrue(r.get("active")),cb.lessThanOrEqualTo(r.get("startsAt"),n),cb.greaterThan(r.get("expiresAt"),n));case "INACTIVE"->(r,q,cb)->cb.and(cb.isFalse(r.get("active")),cb.greaterThan(r.get("expiresAt"),n));case "EXPIRED"->(r,q,cb)->cb.lessThanOrEqualTo(r.get("expiresAt"),n);case "SCHEDULED"->(r,q,cb)->cb.and(cb.isTrue(r.get("active")),cb.greaterThan(r.get("startsAt"),n));default->throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported coupon status.");};}
	private void validateRequest(CouponRequest r){if(!r.expiresAt().isAfter(r.startsAt()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Expiry must be after start date.");if(r.discountType()==CouponDiscountType.PERCENTAGE&&r.discountValue().compareTo(new BigDecimal("100"))>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Percentage discount cannot exceed 100%.");if(r.discountType()==CouponDiscountType.FIXED_AMOUNT&&r.maximumDiscountAmount()!=null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Maximum discount cap only applies to percentage discounts.");if(r.active()&&!r.expiresAt().isAfter(Instant.now()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Expired coupons cannot be active.");Set<Long> p=safe(r.productIds()),cats=safe(r.categoryIds()),customers=safe(r.customerIds());if(r.scope()==CouponScope.SELECTED_PRODUCTS&&(p.isEmpty()||products.findAllById(p).size()!=p.size()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose existing products for this coupon.");if(r.scope()==CouponScope.SELECTED_CATEGORIES&&(cats.isEmpty()||categories.findAllById(cats).size()!=cats.size()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose existing categories for this coupon.");if(r.eligibility()==CouponEligibility.SELECTED_CUSTOMERS){List<User> selected=users.findAllById(customers);if(customers.isEmpty()||selected.size()!=customers.size()||selected.stream().anyMatch(user->user.getRole()!=Role.CUSTOMER))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose existing customer accounts for this coupon.");}if((r.scope()!=CouponScope.SELECTED_PRODUCTS&&!p.isEmpty())||(r.scope()!=CouponScope.SELECTED_CATEGORIES&&!cats.isEmpty())||(r.eligibility()!=CouponEligibility.SELECTED_CUSTOMERS&&!customers.isEmpty()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Coupon selections do not match the selected rules.");}
	private Coupon entity(CouponRequest r,String code){return new Coupon(code,r.name().trim(),trim(r.description()),trim(r.adminNote()),r.discountType(),r.discountValue(),r.minimumOrderAmount(),r.maximumDiscountAmount(),r.usageLimit(),r.perCustomerLimit(),r.startsAt(),r.expiresAt(),r.active(),r.eligibility(),r.scope(),safe(r.productIds()),safe(r.categoryIds()),safe(r.customerIds()));}
	private CouponResponse response(Coupon c){Instant n=Instant.now();String s=c.isArchived()?"ARCHIVED":!c.getExpiresAt().isAfter(n)?"EXPIRED":!c.isActive()?"INACTIVE":c.getStartsAt().isAfter(n)?"SCHEDULED":"ACTIVE";return new CouponResponse(c.getId(),c.getCode(),c.getName(),c.getDescription(),c.getAdminNote(),c.getDiscountType(),c.getDiscountValue(),c.getMinimumOrderAmount(),c.getMaximumDiscountAmount(),c.getUsageLimit(),c.getPerCustomerLimit(),c.getUsageCount(),c.getStartsAt(),c.getExpiresAt(),c.isActive(),c.isArchived(),s,c.getEligibility(),c.getScope(),Set.copyOf(c.getProductIds()),Set.copyOf(c.getCategoryIds()),Set.copyOf(c.getCustomerIds()));}
	private Coupon findCoupon(Long id){return coupons.findById(id).filter(c->!c.isArchived()).orElseThrow(this::notFound);}
	private CouponDiscountType parseType(String s){try{return CouponDiscountType.valueOf(s.toUpperCase(Locale.ROOT));}catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported discount type.");}}
	private String normalize(String s){return s.trim().toUpperCase(Locale.ROOT);} private String trim(String s){return s==null||s.isBlank()?null:s.trim();} private Set<Long> safe(Set<Long>s){return s==null?Set.of():new HashSet<>(s);}
	private void validatePage(int p,int s){if(p<0||s<1||s>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid page or size.");}
	private ResponseStatusException notFound(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Coupon was not found.");} private ResponseStatusException conflict(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);} private ResponseStatusException unprocessable(String m){return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,m);}
	public record CouponLine(Long productId,Long categoryId,BigDecimal subtotal){} public record CouponQuote(Coupon coupon,BigDecimal discount){}
}

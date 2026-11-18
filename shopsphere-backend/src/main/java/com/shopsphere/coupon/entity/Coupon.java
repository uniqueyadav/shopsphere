package com.shopsphere.coupon.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.common.enums.CouponDiscountType;
import com.shopsphere.common.enums.CouponEligibility;
import com.shopsphere.common.enums.CouponScope;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "coupons", uniqueConstraints = @UniqueConstraint(name = "uk_coupons_code", columnNames = "coupon_code"))
public class Coupon extends BaseEntity {
	@Column(name="coupon_code", nullable=false, length=40)
	private String code;
	@Column(name="coupon_name", nullable=false, length=120)
	private String name;
	@Column(length=500)
	private String description;
	@Column(name="admin_note", length=500)
	private String adminNote;
	@Enumerated(EnumType.STRING) @Column(name="discount_type", nullable=false, length=20)
	private CouponDiscountType discountType;
	@Column(name="discount_value", nullable=false, precision=12, scale=2)
	private BigDecimal discountValue;
	@Column(name="minimum_order_amount", nullable=false, precision=12, scale=2)
	private BigDecimal minimumOrderAmount;
	@Column(name="maximum_discount_amount", precision=12, scale=2)
	private BigDecimal maximumDiscountAmount;
	@Column(name="usage_limit")
	private Integer usageLimit;
	@Column(name="per_customer_limit", nullable=false)
	private Integer perCustomerLimit;
	@Column(name="usage_count", nullable=false)
	private int usageCount;
	@Column(name="starts_at", nullable=false)
	private Instant startsAt;
	@Column(name="expires_at", nullable=false)
	private Instant expiresAt;
	@Column(name="is_active", nullable=false)
	private boolean active;
	@Column(name="is_archived", nullable=false)
	private boolean archived;
	@Enumerated(EnumType.STRING) @Column(name="customer_eligibility", nullable=false, length=24)
	private CouponEligibility eligibility;
	@Enumerated(EnumType.STRING) @Column(name="coupon_scope", nullable=false, length=24)
	private CouponScope scope;
	@ElementCollection(fetch=FetchType.LAZY) @CollectionTable(name="coupon_product_rules", joinColumns=@JoinColumn(name="coupon_id", foreignKey=@ForeignKey(name="fk_coupon_product_rules_coupon")))
	@Column(name="product_id", nullable=false)
	private Set<Long> productIds = new LinkedHashSet<>();
	@ElementCollection(fetch=FetchType.LAZY) @CollectionTable(name="coupon_category_rules", joinColumns=@JoinColumn(name="coupon_id", foreignKey=@ForeignKey(name="fk_coupon_category_rules_coupon")))
	@Column(name="category_id", nullable=false)
	private Set<Long> categoryIds = new LinkedHashSet<>();
	@ElementCollection(fetch=FetchType.LAZY) @CollectionTable(name="coupon_customer_rules", joinColumns=@JoinColumn(name="coupon_id", foreignKey=@ForeignKey(name="fk_coupon_customer_rules_coupon")))
	@Column(name="customer_id", nullable=false)
	private Set<Long> customerIds = new LinkedHashSet<>();

	protected Coupon() { }
	public Coupon(String code, String name, String description, String adminNote, CouponDiscountType discountType, BigDecimal discountValue, BigDecimal minimumOrderAmount, BigDecimal maximumDiscountAmount, Integer usageLimit, Integer perCustomerLimit, Instant startsAt, Instant expiresAt, boolean active, CouponEligibility eligibility, CouponScope scope, Set<Long> productIds, Set<Long> categoryIds, Set<Long> customerIds) {
		this.code=code; this.name=name; this.description=description; this.adminNote=adminNote; this.discountType=discountType; this.discountValue=discountValue; this.minimumOrderAmount=minimumOrderAmount; this.maximumDiscountAmount=maximumDiscountAmount; this.usageLimit=usageLimit; this.perCustomerLimit=perCustomerLimit; this.startsAt=startsAt; this.expiresAt=expiresAt; this.active=active; this.eligibility=eligibility; this.scope=scope; this.productIds=new LinkedHashSet<>(productIds); this.categoryIds=new LinkedHashSet<>(categoryIds); this.customerIds=new LinkedHashSet<>(customerIds);
	}
	public String getCode(){return code;} public String getName(){return name;} public String getDescription(){return description;} public String getAdminNote(){return adminNote;} public CouponDiscountType getDiscountType(){return discountType;} public BigDecimal getDiscountValue(){return discountValue;} public BigDecimal getMinimumOrderAmount(){return minimumOrderAmount;} public BigDecimal getMaximumDiscountAmount(){return maximumDiscountAmount;} public Integer getUsageLimit(){return usageLimit;} public Integer getPerCustomerLimit(){return perCustomerLimit;} public int getUsageCount(){return usageCount;} public Instant getStartsAt(){return startsAt;} public Instant getExpiresAt(){return expiresAt;} public boolean isActive(){return active;} public boolean isArchived(){return archived;} public CouponEligibility getEligibility(){return eligibility;} public CouponScope getScope(){return scope;} public Set<Long> getProductIds(){return productIds;} public Set<Long> getCategoryIds(){return categoryIds;} public Set<Long> getCustomerIds(){return customerIds;}
	public void update(String code, String name, String description, String adminNote, CouponDiscountType type, BigDecimal value, BigDecimal minimum, BigDecimal maximum, Integer limit, Integer perCustomer, Instant starts, Instant expires, boolean active, CouponEligibility eligibility, CouponScope scope, Set<Long> products, Set<Long> categories, Set<Long> customers) { this.code=code; this.name=name; this.description=description; this.adminNote=adminNote; this.discountType=type; this.discountValue=value; this.minimumOrderAmount=minimum; this.maximumDiscountAmount=maximum; this.usageLimit=limit; this.perCustomerLimit=perCustomer; this.startsAt=starts; this.expiresAt=expires; this.active=active; this.eligibility=eligibility; this.scope=scope; this.productIds=new LinkedHashSet<>(products); this.categoryIds=new LinkedHashSet<>(categories); this.customerIds=new LinkedHashSet<>(customers); }
	public void setActive(boolean active){this.active=active;} public void archive(){this.archived=true;this.active=false;} public void recordUse(){usageCount=Math.addExact(usageCount,1);}
}

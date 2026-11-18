package com.shopsphere.product.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.shopsphere.category.entity.Category;
import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.common.enums.ProductStatus;
import com.shopsphere.seller.entity.Seller;
import com.shopsphere.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "products",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_products_slug", columnNames = "slug"),
				@UniqueConstraint(name = "uk_products_sku", columnNames = "sku")
		},
		indexes = {
				@Index(name = "idx_products_marketplace", columnList = "status,is_active,category_id"),
				@Index(name = "idx_products_seller", columnList = "seller_id,created_at")
		})
public class Product extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seller_id", nullable = false, foreignKey = @ForeignKey(name = "fk_products_seller"))
	private Seller seller;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_products_category"))
	private Category category;

	@Column(name = "product_name", nullable = false, length = 150)
	private String productName;

	@Column(nullable = false, length = 180)
	private String slug;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(length = 100)
	private String brand;

	@Column(nullable = false, length = 100)
	private String sku;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(name = "discount_price", precision = 12, scale = 2)
	private BigDecimal discountPrice;

	@Column(name = "stock_quantity", nullable = false)
	private int stockQuantity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProductStatus status = ProductStatus.DRAFT;

	@Column(name = "is_active", nullable = false)
	private boolean isActive = true;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reviewed_by", foreignKey = @ForeignKey(name = "fk_products_reviewed_by"))
	private User reviewedBy;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@Column(name = "rejection_reason", length = 1000)
	private String rejectionReason;

	protected Product() {
	}

	public Product(
			Seller seller,
			Category category,
			String productName,
			String slug,
			String description,
			String brand,
			String sku,
			BigDecimal price,
			BigDecimal discountPrice,
			int stockQuantity) {
		this.seller = seller;
		this.category = category;
		this.productName = productName;
		this.slug = slug;
		this.description = description;
		this.brand = brand;
		this.sku = sku;
		this.price = price;
		this.discountPrice = discountPrice;
		this.stockQuantity = stockQuantity;
	}

	public Seller getSeller() {
		return seller;
	}

	public Category getCategory() {
		return category;
	}

	public String getProductName() {
		return productName;
	}

	public String getSlug() {
		return slug;
	}

	public String getDescription() {
		return description;
	}

	public String getBrand() {
		return brand;
	}

	public String getSku() {
		return sku;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public BigDecimal getDiscountPrice() {
		return discountPrice;
	}

	public int getStockQuantity() {
		return stockQuantity;
	}

	public void setStockQuantity(int stockQuantity) {
		if (stockQuantity < 0) {
			throw new IllegalArgumentException("Stock quantity cannot be negative.");
		}
		this.stockQuantity = stockQuantity;
	}

	public void decreaseStock(int quantity) {
		if (quantity < 1 || quantity > stockQuantity) {
			throw new IllegalArgumentException("Quantity exceeds available product stock.");
		}
		stockQuantity -= quantity;
	}

	public void increaseStock(int quantity) {
		if (quantity < 1) {
			throw new IllegalArgumentException("Quantity must be positive.");
		}
		stockQuantity = Math.addExact(stockQuantity, quantity);
	}

	public ProductStatus getStatus() {
		return status;
	}

	public boolean isActive() {
		return isActive;
	}

	public User getReviewedBy() {
		return reviewedBy;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}

	public String getRejectionReason() {
		return rejectionReason;
	}

	public void update(
			Category category,
			String productName,
			String slug,
			String description,
			String brand,
			String sku,
			BigDecimal price,
			BigDecimal discountPrice,
			int stockQuantity) {
		this.category = category;
		this.productName = productName;
		this.slug = slug;
		this.description = description;
		this.brand = brand;
		this.sku = sku;
		this.price = price;
		this.discountPrice = discountPrice;
		this.stockQuantity = stockQuantity;
		if (status != ProductStatus.DRAFT) {
			status = ProductStatus.DRAFT;
			clearReview();
		}
	}

	public void submitForApproval() {
		status = ProductStatus.PENDING;
		clearReview();
	}

	public void approve(User reviewer, Instant reviewTime) {
		status = ProductStatus.APPROVED;
		reviewedBy = reviewer;
		reviewedAt = reviewTime;
		rejectionReason = null;
	}

	public void reject(User reviewer, Instant reviewTime, String reason) {
		status = ProductStatus.REJECTED;
		reviewedBy = reviewer;
		reviewedAt = reviewTime;
		rejectionReason = reason;
	}

	public void deactivate() {
		isActive = false;
	}

	public void activate() {
		isActive = true;
	}

	private void clearReview() {
		reviewedBy = null;
		reviewedAt = null;
		rejectionReason = null;
	}
}

package com.shopsphere.product.entity;

import com.shopsphere.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "product_images",
		uniqueConstraints = @UniqueConstraint(name = "uk_product_images_public_id", columnNames = "public_id"),
		indexes = @Index(name = "idx_product_images_order", columnList = "product_id,display_order"))
public class ProductImage extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_images_product"))
	private Product product;

	@Column(nullable = false, length = 2048)
	private String url;

	@Column(name = "public_id", nullable = false, length = 255)
	private String publicId;

	@Column(name = "is_primary", nullable = false)
	private boolean isPrimary;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	protected ProductImage() {
	}

	public ProductImage(Product product, String url, String publicId, int displayOrder, boolean isPrimary) {
		this.product = product;
		this.url = url;
		this.publicId = publicId;
		this.displayOrder = displayOrder;
		this.isPrimary = isPrimary;
	}

	public Product getProduct() {
		return product;
	}

	public String getUrl() {
		return url;
	}

	public String getPublicId() {
		return publicId;
	}

	public boolean isPrimary() {
		return isPrimary;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public void setPrimary(boolean primary) {
		isPrimary = primary;
	}

	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}
}
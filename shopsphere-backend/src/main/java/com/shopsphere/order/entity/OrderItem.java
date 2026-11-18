package com.shopsphere.order.entity;

import java.math.BigDecimal;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.product.entity.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "seller_order_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_order_items_seller_order"))
	private SellerOrder sellerOrder;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_items_product"))
	private Product product;

	@Column(name = "product_name", nullable = false, length = 150)
	private String productName;

	@Column(nullable = false, length = 100)
	private String sku;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "line_subtotal", nullable = false, precision = 12, scale = 2)
	private BigDecimal lineSubtotal;

	protected OrderItem() {
	}

	public OrderItem(
			SellerOrder sellerOrder,
			Product product,
			String productName,
			String sku,
			int quantity,
			BigDecimal unitPrice) {
		this.sellerOrder = sellerOrder;
		this.product = product;
		this.productName = productName;
		this.sku = sku;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	public SellerOrder getSellerOrder() {
		return sellerOrder;
	}

	public Product getProduct() {
		return product;
	}

	public String getProductName() {
		return productName;
	}

	public String getSku() {
		return sku;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public BigDecimal getLineSubtotal() {
		return lineSubtotal;
	}
}
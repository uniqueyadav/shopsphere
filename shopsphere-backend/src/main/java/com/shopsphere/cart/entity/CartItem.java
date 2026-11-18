package com.shopsphere.cart.entity;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.product.entity.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "cart_items",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_cart_items_cart_product",
				columnNames = {"cart_id", "product_id"}))
public class CartItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cart_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cart_items_cart"))
	private Cart cart;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cart_items_product"))
	private Product product;

	@Column(nullable = false)
	private int quantity;

	protected CartItem() {
	}

	CartItem(Cart cart, Product product, int quantity) {
		this.cart = cart;
		this.product = product;
		updateQuantity(quantity);
	}

	public Cart getCart() {
		return cart;
	}

	public Product getProduct() {
		return product;
	}

	public int getQuantity() {
		return quantity;
	}

	public void updateQuantity(int quantity) {
		if (quantity < 1) {
			throw new IllegalArgumentException("Cart item quantity must be positive.");
		}
		this.quantity = quantity;
	}

	void detachFromCart() {
		cart = null;
	}
}
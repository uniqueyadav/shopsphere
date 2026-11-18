package com.shopsphere.wishlist.entity;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.product.entity.Product;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "wishlist_items",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_wishlist_items_wishlist_product",
				columnNames = {"wishlist_id", "product_id"}))
public class WishlistItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "wishlist_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_wishlist_items_wishlist"))
	private Wishlist wishlist;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "product_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_wishlist_items_product"))
	private Product product;

	protected WishlistItem() {
	}

	WishlistItem(Wishlist wishlist, Product product) {
		this.wishlist = wishlist;
		this.product = product;
	}

	public Wishlist getWishlist() {
		return wishlist;
	}

	public Product getProduct() {
		return product;
	}

	void detachFromWishlist() {
		wishlist = null;
	}
}

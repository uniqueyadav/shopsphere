package com.shopsphere.wishlist.entity;

import java.util.ArrayList;
import java.util.List;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.product.entity.Product;
import com.shopsphere.user.entity.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "wishlists",
		uniqueConstraints = @UniqueConstraint(name = "uk_wishlists_customer", columnNames = "customer_id"))
public class Wishlist extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "customer_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_wishlists_customer"))
	private User customer;

	@OneToMany(mappedBy = "wishlist", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WishlistItem> items = new ArrayList<>();

	protected Wishlist() {
	}

	public Wishlist(User customer) {
		this.customer = customer;
	}

	public User getCustomer() {
		return customer;
	}

	public List<WishlistItem> getItems() {
		return items;
	}

	public WishlistItem addItem(Product product) {
		WishlistItem item = new WishlistItem(this, product);
		items.add(item);
		return item;
	}

	public void removeItem(WishlistItem item) {
		if (items.remove(item)) {
			item.detachFromWishlist();
		}
	}

	public void clearItems() {
		for (WishlistItem item : List.copyOf(items)) {
			removeItem(item);
		}
	}
}
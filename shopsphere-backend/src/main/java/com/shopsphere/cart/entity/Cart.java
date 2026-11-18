package com.shopsphere.cart.entity;

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
		name = "carts",
		uniqueConstraints = @UniqueConstraint(name = "uk_carts_customer", columnNames = "customer_id"))
public class Cart extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "customer_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_carts_customer"))
	private User customer;

	@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CartItem> items = new ArrayList<>();

	protected Cart() {
	}

	public Cart(User customer) {
		this.customer = customer;
	}

	public User getCustomer() {
		return customer;
	}

	public List<CartItem> getItems() {
		return items;
	}

	public CartItem addItem(Product product, int quantity) {
		CartItem item = new CartItem(this, product, quantity);
		items.add(item);
		return item;
	}

	public void removeItem(CartItem item) {
		if (items.remove(item)) {
			item.detachFromCart();
		}
	}

	public void clearItems() {
		for (CartItem item : List.copyOf(items)) {
			removeItem(item);
		}
	}
}
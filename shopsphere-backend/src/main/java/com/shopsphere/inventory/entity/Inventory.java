package com.shopsphere.inventory.entity;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.product.entity.Product;
import com.shopsphere.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A durable record of a stock quantity change. The current quantity remains on Product. */
@Entity
@Table(name = "inventory_history")
public class Inventory extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inventory_history_product"))
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "changed_by_user_id", foreignKey = @ForeignKey(name = "fk_inventory_history_user"))
	private User changedBy;

	@Column(name = "previous_quantity", nullable = false)
	private int previousQuantity;

	@Column(name = "new_quantity", nullable = false)
	private int newQuantity;

	@Column(name = "change_type", nullable = false, length = 32)
	private String changeType;

	protected Inventory() {}

	public Inventory(Product product, User changedBy, int previousQuantity, int newQuantity, String changeType) {
		this.product = product;
		this.changedBy = changedBy;
		this.previousQuantity = previousQuantity;
		this.newQuantity = newQuantity;
		this.changeType = changeType;
	}

	public Product getProduct() { return product; }
	public User getChangedBy() { return changedBy; }
	public int getPreviousQuantity() { return previousQuantity; }
	public int getNewQuantity() { return newQuantity; }
	public String getChangeType() { return changeType; }
}

package com.shopsphere.coupon.entity;

import java.math.BigDecimal;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.order.entity.Order;
import com.shopsphere.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name="coupon_usages", uniqueConstraints=@UniqueConstraint(name="uk_coupon_usage_order", columnNames={"coupon_id","order_id"}))
public class CouponUsage extends BaseEntity {
	@ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="coupon_id",nullable=false,foreignKey=@ForeignKey(name="fk_coupon_usage_coupon")) private Coupon coupon;
	@ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="order_id",nullable=false,foreignKey=@ForeignKey(name="fk_coupon_usage_order")) private Order order;
	@ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="customer_id",nullable=false,foreignKey=@ForeignKey(name="fk_coupon_usage_customer")) private User customer;
	@Column(name="discount_amount",nullable=false,precision=12,scale=2) private BigDecimal discountAmount;
	protected CouponUsage() { }
	public CouponUsage(Coupon coupon, Order order, User customer, BigDecimal discountAmount){this.coupon=coupon;this.order=order;this.customer=customer;this.discountAmount=discountAmount;}
	public Coupon getCoupon(){return coupon;} public Order getOrder(){return order;} public User getCustomer(){return customer;} public BigDecimal getDiscountAmount(){return discountAmount;}
}

package com.shopsphere.analytics.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.common.enums.Role;
import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.order.entity.Order;
import com.shopsphere.order.entity.OrderItem;
import com.shopsphere.product.entity.ProductImage;
import com.shopsphere.seller.entity.Seller;
import com.shopsphere.user.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class AnalyticsRepository {
	@PersistenceContext private EntityManager entityManager;

	public long countUsers(Role role) {
		return entityManager.createQuery("select count(u) from User u where u.role = :role", Long.class)
				.setParameter("role", role).getSingleResult();
	}

	public long countUsers(Role role, Instant start, Instant end) {
		return entityManager.createQuery("select count(u) from User u where u.role = :role and u.createdAt >= :start and u.createdAt < :end", Long.class)
				.setParameter("role", role).setParameter("start", start).setParameter("end", end).getSingleResult();
	}

	public List<Instant> userCreatedAt(Role role, Instant start, Instant end) {
		return entityManager.createQuery("select u.createdAt from User u where u.role = :role and u.createdAt >= :start and u.createdAt < :end", Instant.class)
				.setParameter("role", role).setParameter("start", start).setParameter("end", end).getResultList();
	}

	public long countSellers(SellerStatus status) {
		return entityManager.createQuery("select count(s) from Seller s where s.approvalStatus = :status", Long.class)
				.setParameter("status", status).getSingleResult();
	}

	public long countSellers(SellerStatus status, Instant start, Instant end) {
		return entityManager.createQuery("select count(s) from Seller s where s.approvalStatus = :status and s.createdAt >= :start and s.createdAt < :end", Long.class)
				.setParameter("status", status).setParameter("start", start).setParameter("end", end).getSingleResult();
	}

	public List<Instant> sellerCreatedAt(SellerStatus status, Instant start, Instant end) {
		return entityManager.createQuery("select s.createdAt from Seller s where s.approvalStatus = :status and s.createdAt >= :start and s.createdAt < :end", Instant.class)
				.setParameter("status", status).setParameter("start", start).setParameter("end", end).getResultList();
	}

	public long countOrders(Instant start, Instant end) {
		return entityManager.createQuery("select count(o) from Order o where o.createdAt >= :start and o.createdAt < :end", Long.class)
				.setParameter("start", start).setParameter("end", end).getSingleResult();
	}

	public BigDecimal sumRevenue(Instant start, Instant end) {
		BigDecimal value = entityManager.createQuery(
				"select sum(o.totalAmount) from Order o where o.paymentStatus = :paid and o.createdAt >= :start and o.createdAt < :end", BigDecimal.class)
				.setParameter("paid", PaymentStatus.SUCCESS).setParameter("start", start).setParameter("end", end).getSingleResult();
		return value == null ? BigDecimal.ZERO : value;
	}

	public long countPaidOrders(Instant start, Instant end) {
		return entityManager.createQuery("select count(o) from Order o where o.paymentStatus = :paid and o.createdAt >= :start and o.createdAt < :end", Long.class)
				.setParameter("paid", PaymentStatus.SUCCESS).setParameter("start", start).setParameter("end", end).getSingleResult();
	}

	public List<Object[]> paidRevenueByDay(Instant start, Instant end) {
		return entityManager.createQuery("select function('date', o.createdAt), sum(o.totalAmount), count(o) from Order o where o.paymentStatus = :paid and o.createdAt >= :start and o.createdAt < :end group by function('date', o.createdAt) order by function('date', o.createdAt)", Object[].class)
				.setParameter("paid", PaymentStatus.SUCCESS).setParameter("start", start).setParameter("end", end).getResultList();
	}

	public List<Object[]> recentPaidOrderFacts(Instant start, Instant end, int limit) {
		return entityManager.createQuery("select o.id, o.customer.fullName, o.totalAmount, o.paymentStatus, o.orderStatus, o.createdAt from Order o where o.paymentStatus = :paid and o.createdAt >= :start and o.createdAt < :end order by o.createdAt desc", Object[].class)
				.setParameter("paid", PaymentStatus.SUCCESS).setParameter("start", start).setParameter("end", end).setMaxResults(limit).getResultList();
	}

	public List<Object[]> paidCategoryRevenue(Instant start, Instant end, int limit) {
		return entityManager.createQuery("select c.id, c.name, c.imageUrl, sum(i.lineSubtotal) from OrderItem i join i.product p join p.category c join i.sellerOrder so join so.order o where o.paymentStatus = :paid and o.createdAt >= :start and o.createdAt < :end group by c.id, c.name, c.imageUrl order by sum(i.lineSubtotal) desc", Object[].class)
				.setParameter("paid", PaymentStatus.SUCCESS).setParameter("start", start).setParameter("end", end).setMaxResults(limit).getResultList();
	}

	public List<Order> recentOrders(Instant start, Instant end, int limit) {
		return entityManager.createQuery("select o from Order o join fetch o.customer where o.createdAt >= :start and o.createdAt < :end order by o.createdAt desc", Order.class)
				.setParameter("start", start).setParameter("end", end).setMaxResults(limit).getResultList();
	}

	public List<Object[]> orderFacts(Instant start, Instant end) {
		return entityManager.createQuery("select o.createdAt, o.totalAmount, o.paymentStatus, o.orderStatus from Order o where o.createdAt >= :start and o.createdAt < :end", Object[].class)
				.setParameter("start", start).setParameter("end", end).getResultList();
	}

	public List<Object[]> statusCounts(Instant start, Instant end) {
		return entityManager.createQuery("select o.orderStatus, count(o) from Order o where o.createdAt >= :start and o.createdAt < :end group by o.orderStatus", Object[].class)
				.setParameter("start", start).setParameter("end", end).getResultList();
	}

	public List<Object[]> topProducts(Instant start, Instant end, int limit) {
		return entityManager.createQuery("select i.product.id, i.product.productName, sum(i.quantity), sum(i.lineSubtotal) from OrderItem i join i.sellerOrder so join so.order o where o.paymentStatus = :paid and o.createdAt >= :start and o.createdAt < :end group by i.product.id, i.product.productName order by sum(i.quantity) desc", Object[].class)
				.setParameter("paid", PaymentStatus.SUCCESS).setParameter("start", start).setParameter("end", end).setMaxResults(limit).getResultList();
	}

	public List<Object[]> productImages(List<Long> ids) {
		if (ids.isEmpty()) return List.of();
		return entityManager.createQuery("select image.product.id, image.url from ProductImage image where image.product.id in :ids and image.isPrimary = true", Object[].class)
				.setParameter("ids", ids).getResultList();
	}
}

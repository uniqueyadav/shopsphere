package com.shopsphere.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shopsphere.analytics.dto.SellerDashboardResponse;
import com.shopsphere.analytics.dto.SellerDashboardResponse.Metric;
import com.shopsphere.analytics.dto.SellerDashboardResponse.RecentOrder;
import com.shopsphere.analytics.dto.SellerDashboardResponse.TopProduct;
import com.shopsphere.analytics.dto.SellerDashboardResponse.TrendPoint;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.common.enums.ProductStatus;
import com.shopsphere.common.enums.ReviewStatus;
import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.seller.entity.Seller;
import com.shopsphere.seller.repository.SellerRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class SellerAnalyticsService {
	private static final Set<PaymentStatus> PAID = Set.of(PaymentStatus.SUCCESS, PaymentStatus.PAID);
	private final SellerRepository sellers;
	@PersistenceContext private EntityManager entityManager;

	public SellerAnalyticsService(SellerRepository sellers) {
		this.sellers = sellers;
	}

	@Transactional(readOnly = true)
	public SellerDashboardResponse getDashboard(Long userId, String requestedPeriod) {
		Seller seller = sellers.findByUser_Id(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller profile was not found."));
		if (seller.getApprovalStatus() != SellerStatus.APPROVED) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only approved sellers can view dashboard analytics.");
		}

		Period period = Period.from(requestedPeriod);
		Instant now = Instant.now();
		Instant from = period.allTime ? Instant.EPOCH : now.minus(period.duration);
		Instant previousFrom = period.allTime ? null : from.minus(period.duration);
		BigDecimal sales = sumSales(seller.getId(), from, now);
		BigDecimal previousSales = previousFrom == null ? null : sumSales(seller.getId(), previousFrom, from);
		long orderCount = countOrders(seller.getId(), from, now);
		Long previousOrderCount = previousFrom == null ? null : countOrders(seller.getId(), previousFrom, from);
		Object[] rating = (Object[]) entityManager.createQuery(
				"select avg(review.rating), count(review) from Review review where review.product.seller.id = :sellerId and review.status = :status")
				.setParameter("sellerId", seller.getId()).setParameter("status", ReviewStatus.APPROVED).getSingleResult();
		Double averageRating = rating[0] == null ? null : ((Number) rating[0]).doubleValue();
		long reviewCount = ((Number) rating[1]).longValue();
		long productCount = entityManager.createQuery("select count(product) from Product product where product.seller.id = :sellerId", Long.class)
				.setParameter("sellerId", seller.getId()).getSingleResult();
		long lowStockCount = entityManager.createQuery(
				"select count(product) from Product product where product.seller.id = :sellerId and product.stockQuantity <= 5 and product.status = :status", Long.class)
				.setParameter("sellerId", seller.getId()).setParameter("status", ProductStatus.APPROVED).getSingleResult();

		return new SellerDashboardResponse(
				seller.getStoreName(),
				seller.getSellerFullName() == null || seller.getSellerFullName().isBlank() ? seller.getUser().getFullName() : seller.getSellerFullName(),
				period.name,
				 now,
				new Metric(sales, growth(sales, previousSales)),
				new Metric(BigDecimal.valueOf(orderCount), growth(BigDecimal.valueOf(orderCount), previousOrderCount == null ? null : BigDecimal.valueOf(previousOrderCount))),
				null,
				averageRating,
				reviewCount,
				productCount,
				lowStockCount,
				trend(seller.getId(), from, now),
				topProducts(seller.getId(), from, now),
				recentOrders(seller.getId(), from, now));
	}

	private BigDecimal sumSales(Long sellerId, Instant from, Instant to) {
		return entityManager.createQuery(
				"select coalesce(sum(sellerOrder.subtotal), 0) from SellerOrder sellerOrder where sellerOrder.seller.id = :sellerId and sellerOrder.order.paymentStatus in :paid and sellerOrder.order.createdAt >= :from and sellerOrder.order.createdAt < :to", BigDecimal.class)
				.setParameter("sellerId", sellerId).setParameter("paid", PAID).setParameter("from", from).setParameter("to", to).getSingleResult();
	}

	private long countOrders(Long sellerId, Instant from, Instant to) {
		return entityManager.createQuery(
				"select count(sellerOrder) from SellerOrder sellerOrder where sellerOrder.seller.id = :sellerId and sellerOrder.order.createdAt >= :from and sellerOrder.order.createdAt < :to", Long.class)
				.setParameter("sellerId", sellerId).setParameter("from", from).setParameter("to", to).getSingleResult();
	}

	private List<TrendPoint> trend(Long sellerId, Instant from, Instant to) {
		List<Object[]> rows = entityManager.createQuery(
				"select function('date', sellerOrder.order.createdAt), coalesce(sum(sellerOrder.subtotal), 0), count(sellerOrder) from SellerOrder sellerOrder where sellerOrder.seller.id = :sellerId and sellerOrder.order.paymentStatus in :paid and sellerOrder.order.createdAt >= :from and sellerOrder.order.createdAt < :to group by function('date', sellerOrder.order.createdAt) order by function('date', sellerOrder.order.createdAt)", Object[].class)
				.setParameter("sellerId", sellerId).setParameter("paid", PAID).setParameter("from", from).setParameter("to", to).getResultList();
		return rows.stream().map(row -> new TrendPoint(row[0].toString(), (BigDecimal) row[1], ((Number) row[2]).longValue())).toList();
	}

	private List<TopProduct> topProducts(Long sellerId, Instant from, Instant to) {
		List<Object[]> rows = entityManager.createQuery(
				"select item.product.id, item.product.productName, sum(item.quantity), sum(item.lineSubtotal) from OrderItem item where item.sellerOrder.seller.id = :sellerId and item.sellerOrder.order.paymentStatus in :paid and item.sellerOrder.order.createdAt >= :from and item.sellerOrder.order.createdAt < :to group by item.product.id, item.product.productName order by sum(item.quantity) desc", Object[].class)
				.setParameter("sellerId", sellerId).setParameter("paid", PAID).setParameter("from", from).setParameter("to", to).setMaxResults(5).getResultList();
		if (rows.isEmpty()) return List.of();
		List<Long> productIds = rows.stream().map(row -> (Long) row[0]).toList();
		List<Object[]> imageRows = entityManager.createQuery(
				"select image.product.id, image.url from ProductImage image where image.product.id in :ids and image.isPrimary = true", Object[].class)
				.setParameter("ids", productIds).getResultList();
		Map<Long, String> images = new HashMap<>();
		imageRows.forEach(row -> images.put((Long) row[0], (String) row[1]));
		return rows.stream().map(row -> new TopProduct((Long) row[0], (String) row[1], images.get((Long) row[0]), ((Number) row[2]).longValue(), (BigDecimal) row[3])).toList();
	}

	private List<RecentOrder> recentOrders(Long sellerId, Instant from, Instant to) {
		List<Object[]> rows = entityManager.createQuery(
				"select sellerOrder.id, sellerOrder.order.id, sellerOrder.order.customer.fullName, sellerOrder.totalAmount, sellerOrder.orderStatus, sellerOrder.order.createdAt from SellerOrder sellerOrder where sellerOrder.seller.id = :sellerId and sellerOrder.order.createdAt >= :from and sellerOrder.order.createdAt < :to order by sellerOrder.order.createdAt desc, sellerOrder.id desc", Object[].class)
				.setParameter("sellerId", sellerId).setParameter("from", from).setParameter("to", to).setMaxResults(5).getResultList();
		return rows.stream().map(row -> new RecentOrder((Long) row[0], (Long) row[1], (String) row[2], (BigDecimal) row[3], row[4].toString(), (Instant) row[5])).toList();
	}

	private BigDecimal growth(BigDecimal current, BigDecimal previous) {
		if (previous == null || previous.signum() == 0) return null;
		return current.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 1, RoundingMode.HALF_UP);
	}

	private record Period(String name, Duration duration, boolean allTime) {
		static Period from(String value) {
			if (value == null) return new Period("THIRTY_DAYS", Duration.ofDays(30), false);
			return switch (value.toUpperCase()) {
				case "SEVEN_DAYS" -> new Period("SEVEN_DAYS", Duration.ofDays(7), false);
				case "SIX_MONTHS" -> new Period("SIX_MONTHS", Duration.ofDays(183), false);
				case "ONE_YEAR" -> new Period("ONE_YEAR", Duration.ofDays(365), false);
				case "ALL_TIME" -> new Period("ALL_TIME", Duration.ZERO, true);
				default -> new Period("THIRTY_DAYS", Duration.ofDays(30), false);
			};
		}
	}
}

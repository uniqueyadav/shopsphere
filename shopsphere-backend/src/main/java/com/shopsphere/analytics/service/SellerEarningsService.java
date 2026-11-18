package com.shopsphere.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shopsphere.analytics.dto.SellerEarningsResponse;
import com.shopsphere.analytics.dto.SellerEarningsResponse.CategorySales;
import com.shopsphere.analytics.dto.SellerEarningsResponse.Transaction;
import com.shopsphere.commission.entity.CommissionRule;
import com.shopsphere.commission.repository.CommissionRuleRepository;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.common.enums.RefundStatus;
import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.seller.repository.SellerRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class SellerEarningsService {
	private static final Set<PaymentStatus> PAID = Set.of(PaymentStatus.SUCCESS, PaymentStatus.PAID);
	private final SellerRepository sellers;
	private final CommissionRuleRepository commissionRules;
	@PersistenceContext private EntityManager entityManager;

	public SellerEarningsService(SellerRepository sellers, CommissionRuleRepository commissionRules) {
		this.sellers = sellers;
		this.commissionRules = commissionRules;
	}

	@Transactional(readOnly = true)
	public SellerEarningsResponse get(Long userId, Instant requestedFrom, Instant requestedTo) {
		var seller = sellers.findByUser_Id(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller profile was not found."));
		if (seller.getApprovalStatus() != SellerStatus.APPROVED) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only approved sellers can view earnings.");
		Instant to = requestedTo == null ? Instant.now() : requestedTo;
		Instant from = requestedFrom == null ? to.minus(30, ChronoUnit.DAYS) : requestedFrom;
		if (!from.isBefore(to)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must be before end date.");
		List<CommissionRule> rules = commissionRules.findAllByOrderByCreatedAtDesc().stream().filter(CommissionRule::isActive).toList();
		List<Object[]> rows = entityManager.createQuery("select so.id, so.order.id, so.subtotal, so.totalAmount, so.order.createdAt, so.order.refundStatus, so.order.paymentStatus, item.lineSubtotal, category.id, category.name, so.orderStatus " +
				"from SellerOrder so join so.items item join item.product product join product.category category " +
				"where so.seller.id = :sellerId and so.order.paymentStatus in :paid and so.order.createdAt >= :from and so.order.createdAt < :to " +
				"order by so.order.createdAt desc, so.id desc", Object[].class)
				.setParameter("sellerId", seller.getId()).setParameter("paid", PAID).setParameter("from", from).setParameter("to", to).getResultList();

		BigDecimal gross = BigDecimal.ZERO, refunds = BigDecimal.ZERO, sales = BigDecimal.ZERO, commission = BigDecimal.ZERO, earnings = BigDecimal.ZERO;
		boolean commissionKnown = true;
		Map<LocalDate, Day> days = new TreeMap<>();
		Map<Long, Category> categories = new LinkedHashMap<>();
		Map<Long, Tx> txs = new LinkedHashMap<>();
		for (Object[] r : rows) {
			Long sellerOrderId = (Long) r[0], orderId = (Long) r[1];
			BigDecimal subtotal = (BigDecimal) r[2], total = (BigDecimal) r[3], line = (BigDecimal) r[7];
			Instant createdAt = (Instant) r[4]; RefundStatus refundStatus = (RefundStatus) r[5];
			Long categoryId = (Long) r[8]; String categoryName = (String) r[9];
			boolean refunded = refundStatus == RefundStatus.REFUNDED;
			BigDecimal rate = commissionRate(rules, categoryId, createdAt);
			BigDecimal lineCommission = rate == null ? null : line.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
			gross = gross.add(line);
			BigDecimal recognizedSale = refunded ? BigDecimal.ZERO : line;
			sales = sales.add(recognizedSale);
			if (refunded) refunds = refunds.add(line);
			if (lineCommission == null) commissionKnown = false;
			else if (!refunded) { commission = commission.add(lineCommission); earnings = earnings.add(line.subtract(lineCommission)); }
			LocalDate date = createdAt.atZone(java.time.ZoneOffset.UTC).toLocalDate();
			Day day = days.computeIfAbsent(date, ignored -> new Day()); day.sales = day.sales.add(recognizedSale);
			if (lineCommission == null) day.known = false; else if (!refunded) day.earnings = day.earnings.add(line.subtract(lineCommission));
			Category category = categories.computeIfAbsent(categoryId, ignored -> new Category(categoryName)); if (!refunded) category.sales = category.sales.add(line);
			Tx tx = txs.computeIfAbsent(sellerOrderId, ignored -> new Tx(sellerOrderId, orderId, createdAt, total, refundStatus, r[10].toString()));
			tx.productNames.add(categoryName);
		}
		List<SellerEarningsResponse.TrendPoint> trend = days.entrySet().stream().map(e -> new SellerEarningsResponse.TrendPoint(e.getKey().toString(), e.getValue().sales, e.getValue().known ? e.getValue().earnings : null)).toList();
		List<CategorySales> categorySales = categories.entrySet().stream().map(e -> new CategorySales(e.getKey(), e.getValue().name, e.getValue().sales)).toList();
		List<Transaction> transactions = txs.values().stream().map(tx -> new Transaction(tx.sellerOrderId, tx.orderId, tx.date, "Order #SS" + tx.orderId,
				tx.refundStatus == RefundStatus.REFUNDED ? tx.amount.negate() : tx.amount,
				tx.refundStatus == RefundStatus.REFUNDED ? "REFUNDED" : tx.orderStatus)).toList();
		return new SellerEarningsResponse(from, to, gross, refunds, sales, commissionKnown ? commission : null,
				commissionKnown ? earnings : null, null, null, commissionKnown, false, trend, categorySales, transactions);
	}

	private BigDecimal commissionRate(List<CommissionRule> rules, Long categoryId, Instant at) {
		var categoryRule = rules.stream().filter(rule -> rule.getScope() == CommissionRule.Scope.CATEGORY && rule.getCategory() != null
				&& categoryId.equals(rule.getCategory().getId()) && effective(rule, at)).max(Comparator.comparing(CommissionRule::getEffectiveFrom));
		if (categoryRule.isPresent()) return categoryRule.get().getRatePercent();
		return rules.stream().filter(rule -> rule.getScope() == CommissionRule.Scope.PLATFORM && effective(rule, at))
				.max(Comparator.comparing(CommissionRule::getEffectiveFrom)).map(CommissionRule::getRatePercent).orElse(null);
	}
	private boolean effective(CommissionRule rule, Instant at) { return !rule.getEffectiveFrom().isAfter(at) && (rule.getEffectiveTo() == null || rule.getEffectiveTo().isAfter(at)); }
	private static final class Day { BigDecimal sales=BigDecimal.ZERO, earnings=BigDecimal.ZERO; boolean known=true; }
	private static final class Category { final String name; BigDecimal sales=BigDecimal.ZERO; Category(String name){this.name=name;} }
	private static final class Tx { final Long sellerOrderId,orderId; final Instant date; final BigDecimal amount; final RefundStatus refundStatus; final String orderStatus; final Set<String> productNames=new java.util.LinkedHashSet<>(); Tx(Long s,Long o,Instant d,BigDecimal a,RefundStatus r,String status){sellerOrderId=s;orderId=o;date=d;amount=a;refundStatus=r;orderStatus=status;} }
}

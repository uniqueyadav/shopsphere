package com.shopsphere.payment.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.payment.dto.AdminPaymentPageResponse;
import com.shopsphere.payment.dto.AdminPaymentResponse;
import com.shopsphere.payment.dto.AdminPaymentStatsResponse;
import com.shopsphere.payment.entity.Payment;
import com.shopsphere.payment.repository.PaymentRepository;
import com.shopsphere.payment.repository.AdminPaymentAggregate;
import com.shopsphere.common.enums.PaymentMethod;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AdminPaymentService {
	private final PaymentRepository payments;

	public AdminPaymentService(PaymentRepository payments) { this.payments = payments; }

	public AdminPaymentPageResponse list(int page, int size, String search, String status, String method, LocalDate from, LocalDate to) {
		if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or page size.");
		Page<Payment> result = payments.findAll(specification(search, status, method, from, to), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
		return new AdminPaymentPageResponse(result.getContent().stream().map(this::map).toList(), page, size, result.getTotalElements(), result.getTotalPages());
	}

	public AdminPaymentResponse get(Long id) {
		return payments.findById(id).map(this::map).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment transaction was not found."));
	}

	public AdminPaymentStatsResponse stats(String search, String status, String method, LocalDate from, LocalDate to) {
		validateDates(from, to);
		String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
		String normalizedStatus = parseStatus(status) == null ? null : parseStatus(status).name();
		String normalizedMethod = parseMethod(method) == null ? null : parseMethod(method).name();
		List<AdminPaymentAggregate> grouped = payments.summarizeAdminPayments(normalizedSearch, normalizedStatus, normalizedMethod, start(from), endExclusive(to));
		return new AdminPaymentStatsResponse(count(grouped), sum(grouped), count(grouped, PaymentStatus.SUCCESS, PaymentStatus.PAID), sum(grouped, PaymentStatus.SUCCESS, PaymentStatus.PAID), count(grouped, PaymentStatus.PENDING), sum(grouped, PaymentStatus.PENDING), count(grouped, PaymentStatus.FAILED), sum(grouped, PaymentStatus.FAILED), count(grouped, PaymentStatus.REFUNDED), sum(grouped, PaymentStatus.REFUNDED));
	}

	public List<AdminPaymentResponse> export(String search, String status, String method, LocalDate from, LocalDate to) {
		return payments.findAll(specification(search, status, method, from, to), Sort.by(Sort.Direction.DESC, "createdAt")).stream().map(this::map).toList();
	}

	private Specification<Payment> specification(String search, String status, String method, LocalDate from, LocalDate to) {
		Specification<Payment> spec = (root, query, cb) -> {
			root.join("customer", JoinType.LEFT);
			root.join("order", JoinType.LEFT);
			return cb.conjunction();
		};
		if (search != null && !search.isBlank()) spec = spec.and((root, query, cb) -> {
			String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
			var customer = root.join("customer", JoinType.LEFT);
			var order = root.join("order", JoinType.LEFT);
			return cb.or(cb.like(cb.lower(customer.get("fullName")), term), cb.like(cb.lower(customer.get("email")), term), cb.like(order.get("id").as(String.class), term), cb.like(cb.lower(root.get("razorpayPaymentId")), term), cb.like(cb.lower(root.get("razorpayOrderId")), term));
		});
		if (status != null && !status.isBlank()) {
			PaymentStatus parsed = parseStatus(status);
			spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), parsed));
		}
		if (method != null && !method.isBlank()) { PaymentMethod parsed = parseMethod(method); spec = spec.and((root, query, cb) -> cb.equal(root.join("order").get("paymentMethod"), parsed)); }
		if (from != null) { Instant start = start(from); spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), start)); }
		if (to != null) { Instant end = endExclusive(to); spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), end)); }
		validateDates(from, to);
		return spec;
	}

	private AdminPaymentResponse map(Payment p) {
		var order = p.getOrder();
		var customer = p.getCustomer();
		BigDecimal refund = p.getStatus() == PaymentStatus.REFUNDED ? p.getAmount() : BigDecimal.ZERO;
		var sellerOrders = order.getSellerOrders().stream().map(sellerOrder -> new AdminPaymentResponse.SellerOrderSummary(sellerOrder.getId(), sellerOrder.getSellerName(), sellerOrder.getSubtotal(), sellerOrder.getDeliveryCharge(), sellerOrder.getTotalAmount())).toList();
		return new AdminPaymentResponse(p.getId(), order.getId(), customer.getFullName(), customer.getEmail(), customer.getPhone(), order.getPaymentMethod(), p.getAmount(), p.getCurrency(), p.getStatus(), p.getRazorpayPaymentId(), p.getRazorpayOrderId(), p.getRazorpayRefundId(), p.getFailureCode(), order.getRefundStatus(), refund, order.getSubtotal(), order.getDeliveryCharges(), order.getTotalAmount(), sellerOrders, p.getCreatedAt(), p.getUpdatedAt());
	}
	private PaymentStatus parseStatus(String status) { if (status == null || status.isBlank()) return null; try { return PaymentStatus.valueOf(status.toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported payment status."); } }
	private PaymentMethod parseMethod(String method) { if (method == null || method.isBlank()) return null; try { return PaymentMethod.valueOf(method.toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported payment method."); } }
	private void validateDates(LocalDate from, LocalDate to) { if (from != null && to != null && from.isAfter(to)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date cannot be after end date."); }
	private Instant start(LocalDate date) { return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC); }
	private Instant endExclusive(LocalDate date) { return date == null ? null : date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC); }
	private long count(List<AdminPaymentAggregate> groups, PaymentStatus... statuses) { return groups.stream().filter(g -> statuses.length == 0 || hasStatus(g, statuses)).mapToLong(AdminPaymentAggregate::getTotalCount).sum(); }
	private BigDecimal sum(List<AdminPaymentAggregate> groups, PaymentStatus... statuses) { return groups.stream().filter(g -> statuses.length == 0 || hasStatus(g, statuses)).map(AdminPaymentAggregate::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add); }
	private boolean hasStatus(AdminPaymentAggregate group, PaymentStatus... statuses) { for (PaymentStatus status : statuses) if (group.getStatus() == status) return true; return false; }
}

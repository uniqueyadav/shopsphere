package com.shopsphere.refund.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import com.shopsphere.common.enums.PaymentMethod;
import com.shopsphere.common.enums.RefundStatus;
import com.shopsphere.payment.entity.Payment;
import com.shopsphere.refund.dto.AdminRefundPageResponse;
import com.shopsphere.refund.dto.AdminRefundResponse;
import com.shopsphere.refund.dto.AdminRefundStatsResponse;
import com.shopsphere.refund.repository.AdminRefundAggregate;
import com.shopsphere.refund.repository.RefundRepository;
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
public class RefundService {
	private final RefundRepository refunds;

	public RefundService(RefundRepository refunds) { this.refunds = refunds; }

	public AdminRefundPageResponse list(int page, int size, String search, String status, String method, LocalDate from, LocalDate to) {
		validate(page, size, from, to);
		Page<Payment> result = refunds.findAll(specification(search, status, method, from, to), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by(Sort.Direction.DESC, "id"))));
		return new AdminRefundPageResponse(result.getContent().stream().map(this::map).toList(), page, size, result.getTotalElements(), result.getTotalPages());
	}

	public AdminRefundResponse get(Long paymentId) {
		Payment payment = refunds.findById(paymentId).filter(p -> p.getOrder().getRefundStatus() != RefundStatus.NOT_REQUESTED)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Refund record was not found."));
		return map(payment);
	}

	public AdminRefundStatsResponse stats(String search, String status, String method, LocalDate from, LocalDate to) {
		validate(0, 1, from, to);
		RefundStatus parsedStatus = parseStatus(status);
		PaymentMethod parsedMethod = parseMethod(method);
		List<AdminRefundAggregate> groups = refunds.summarize(search == null || search.isBlank() ? null : search.trim(), parsedStatus == null ? null : parsedStatus.name(), parsedMethod == null ? null : parsedMethod.name(), start(from), endExclusive(to));
		return new AdminRefundStatsResponse(count(groups), sum(groups), count(groups, RefundStatus.PENDING), sum(groups, RefundStatus.PENDING), count(groups, RefundStatus.REFUNDED), sum(groups, RefundStatus.REFUNDED), count(groups, RefundStatus.FAILED), sum(groups, RefundStatus.FAILED));
	}

	public List<AdminRefundResponse> export(String search, String status, String method, LocalDate from, LocalDate to) {
		validate(0, 1, from, to);
		return refunds.findAll(specification(search, status, method, from, to), Sort.by(Sort.Direction.DESC, "updatedAt")).stream().map(this::map).toList();
	}

	private Specification<Payment> specification(String search, String status, String method, LocalDate from, LocalDate to) {
		Specification<Payment> spec = (root, query, cb) -> cb.notEqual(root.join("order", JoinType.INNER).get("refundStatus"), RefundStatus.NOT_REQUESTED);
		if (search != null && !search.isBlank()) spec = spec.and((root, query, cb) -> {
			String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
			var customer = root.join("customer", JoinType.INNER);
			var order = root.join("order", JoinType.INNER);
			return cb.or(cb.like(cb.lower(customer.get("fullName")), term), cb.like(cb.lower(customer.get("email")), term), cb.like(order.get("id").as(String.class), term), cb.like(cb.lower(root.get("razorpayPaymentId")), term), cb.like(cb.lower(root.get("razorpayRefundId")), term));
		});
		RefundStatus parsedStatus = parseStatus(status);
		if (parsedStatus != null) spec = spec.and((root, query, cb) -> cb.equal(root.join("order").get("refundStatus"), parsedStatus));
		PaymentMethod parsedMethod = parseMethod(method);
		if (parsedMethod != null) spec = spec.and((root, query, cb) -> cb.equal(root.join("order").get("paymentMethod"), parsedMethod));
		if (from != null) { Instant start = start(from); spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.join("order").get("updatedAt"), start)); }
		if (to != null) { Instant end = endExclusive(to); spec = spec.and((root, query, cb) -> cb.lessThan(root.join("order").get("updatedAt"), end)); }
		return spec;
	}

	private AdminRefundResponse map(Payment payment) {
		var order = payment.getOrder();
		var customer = payment.getCustomer();
		String reason = order.getSellerOrders().stream().flatMap(sellerOrder -> sellerOrder.getStatusHistory().stream())
				.filter(history -> history.getStatus().name().equals("CANCELLED"))
				.map(history -> history.getNote()).filter(note -> note != null && !note.isBlank()).findFirst().orElse(null);
		var items = order.getSellerOrders().stream().flatMap(sellerOrder -> sellerOrder.getItems().stream())
				.map(item -> new AdminRefundResponse.RefundItem(item.getProductName(), item.getSku(), item.getQuantity())).toList();
		return new AdminRefundResponse(payment.getId(), order.getId(), payment.getRazorpayRefundId(), customer.getFullName(), customer.getEmail(), customer.getPhone(), order.getPaymentMethod(), payment.getRazorpayPaymentId(), order.getTotalAmount(), payment.getAmount(), payment.getCurrency(), order.getRefundStatus(), reason, order.getCreatedAt(), order.getUpdatedAt(), items);
	}

	private RefundStatus parseStatus(String value) {
		if (value == null || value.isBlank()) return null;
		try { return RefundStatus.valueOf(value.toUpperCase(Locale.ROOT)); }
		catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported refund status."); }
	}
	private PaymentMethod parseMethod(String value) {
		if (value == null || value.isBlank()) return null;
		try { return PaymentMethod.valueOf(value.toUpperCase(Locale.ROOT)); }
		catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported payment method."); }
	}
	private void validate(int page, int size, LocalDate from, LocalDate to) {
		if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or page size.");
		if (from != null && to != null && from.isAfter(to)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date cannot be after end date.");
	}
	private Instant start(LocalDate date) { return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC); }
	private Instant endExclusive(LocalDate date) { return date == null ? null : date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC); }
	private long count(List<AdminRefundAggregate> groups, RefundStatus... statuses) { return groups.stream().filter(g -> statuses.length == 0 || contains(g, statuses)).mapToLong(AdminRefundAggregate::getTotalCount).sum(); }
	private java.math.BigDecimal sum(List<AdminRefundAggregate> groups, RefundStatus... statuses) { return groups.stream().filter(g -> statuses.length == 0 || contains(g, statuses)).map(AdminRefundAggregate::getTotalAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add); }
	private boolean contains(AdminRefundAggregate group, RefundStatus... statuses) { for (RefundStatus status : statuses) if (group.getStatus() == status) return true; return false; }
}

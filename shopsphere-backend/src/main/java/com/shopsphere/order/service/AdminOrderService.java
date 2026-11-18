package com.shopsphere.order.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.order.dto.AdminOrderItemResponse;
import com.shopsphere.order.dto.AdminOrderPageResponse;
import com.shopsphere.order.dto.AdminOrderResponse;
import com.shopsphere.order.dto.AdminOrderStatsResponse;
import com.shopsphere.order.dto.AdminOrderTimelineEvent;
import com.shopsphere.order.dto.AdminPaymentResponse;
import com.shopsphere.order.dto.AdminSellerOrderResponse;
import com.shopsphere.order.dto.OrderStatusHistoryResponse;
import com.shopsphere.order.dto.ShippingAddressResponse;
import com.shopsphere.order.entity.Order;
import com.shopsphere.order.entity.OrderItem;
import com.shopsphere.order.entity.OrderStatusHistory;
import com.shopsphere.order.entity.SellerOrder;
import com.shopsphere.order.repository.OrderRepository;
import com.shopsphere.payment.entity.Payment;
import com.shopsphere.payment.repository.PaymentRepository;
import com.shopsphere.product.entity.ProductImage;
import com.shopsphere.product.repository.ProductImageRepository;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminOrderService {

	private static final ZoneId MARKETPLACE_ZONE = ZoneId.of("Asia/Kolkata");

	private final OrderRepository orders;
	private final PaymentRepository payments;
	private final ProductImageRepository productImages;

	public AdminOrderService(OrderRepository orders, PaymentRepository payments, ProductImageRepository productImages) {
		this.orders = orders;
		this.payments = payments;
		this.productImages = productImages;
	}

	@Transactional(readOnly = true)
	public AdminOrderPageResponse list(
			int page, int size, String search, OrderStatus orderStatus,
			com.shopsphere.common.enums.PaymentStatus paymentStatus, OrderStatus deliveryStatus,
			LocalDate from, LocalDate to) {
		validateDates(from, to);
		Page<Order> result = orders.findAll(
				filter(search, orderStatus, paymentStatus, deliveryStatus, from, to),
				PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
		List<Long> ids = result.getContent().stream().map(Order::getId).toList();
		if (ids.isEmpty()) {
			return AdminOrderPageResponse.from(new PageImpl<>(List.of(), result.getPageable(), result.getTotalElements()));
		}
		Map<Long, Order> loadedOrders = orders.findAllAdminDetailsByIdIn(ids).stream()
				.collect(Collectors.toMap(Order::getId, order -> order));
		Map<Long, AdminPaymentResponse> latestPayments = latestPayments(ids);
		Map<Long, String> imageUrls = imageUrls(loadedOrders.values());
		return AdminOrderPageResponse.from(result.map(order -> toResponse(
				loadedOrders.get(order.getId()), latestPayments.get(order.getId()), imageUrls)));
	}

	@Transactional(readOnly = true)
	public AdminOrderResponse get(Long orderId) {
		Order order = orders.findAllAdminDetailsByIdIn(List.of(orderId)).stream().findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order was not found."));
		Map<Long, AdminPaymentResponse> latest = latestPayments(List.of(orderId));
		return toResponse(order, latest.get(orderId), imageUrls(List.of(order)));
	}

	@Transactional(readOnly = true)
	public AdminOrderStatsResponse stats() {
		return new AdminOrderStatsResponse(
				orders.count(),
				orders.countByOrderStatus(OrderStatus.DELIVERED),
				orders.countByOrderStatus(OrderStatus.PROCESSING),
				orders.countByOrderStatus(OrderStatus.CANCELLED));
	}

	@Transactional(readOnly = true)
	public String export(
			String search, OrderStatus orderStatus, com.shopsphere.common.enums.PaymentStatus paymentStatus,
			OrderStatus deliveryStatus, LocalDate from, LocalDate to) {
		validateDates(from, to);
		StringBuilder csv = new StringBuilder("Order ID,Customer,Email,Order Status,Payment Method,Payment Status,Refund Status,Subtotal,Delivery Charges,Total,Created At\n");
		orders.findAll(filter(search, orderStatus, paymentStatus, deliveryStatus, from, to),
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
				.forEach(order -> csv.append(order.getId()).append(',')
						.append(csv(order.getCustomer().getFullName())).append(',')
						.append(csv(order.getCustomer().getEmail())).append(',')
						.append(order.getOrderStatus()).append(',').append(order.getPaymentMethod()).append(',')
						.append(order.getPaymentStatus()).append(',').append(order.getRefundStatus()).append(',')
						.append(order.getSubtotal()).append(',').append(order.getDeliveryCharges()).append(',')
						.append(order.getTotalAmount()).append(',').append(order.getCreatedAt()).append('\n'));
		return csv.toString();
	}

	private Map<Long, AdminPaymentResponse> latestPayments(Collection<Long> orderIds) {
	Map<Long, AdminPaymentResponse> result = new HashMap<>();
	for (Payment payment : payments.findByOrder_IdInOrderByCreatedAtDescIdDesc(orderIds)) {
		result.putIfAbsent(payment.getOrder().getId(), AdminPaymentResponse.from(payment));
	}
	return result;
	}

	private Map<Long, String> imageUrls(Collection<Order> orderRows) {
	List<Long> productIds = orderRows.stream()
			.flatMap(order -> order.getSellerOrders().stream())
			.flatMap(sellerOrder -> sellerOrder.getItems().stream())
			.map(item -> item.getProduct().getId())
			.distinct()
			.toList();
	if (productIds.isEmpty()) return Map.of();
	Map<Long, String> result = new HashMap<>();
	for (ProductImage image : productImages.findByProduct_IdInOrderByDisplayOrderAscIdAsc(productIds)) {
		result.putIfAbsent(image.getProduct().getId(), image.getUrl());
	}
	return result;
	}

	private AdminOrderResponse toResponse(Order order, AdminPaymentResponse payment, Map<Long, String> imageUrls) {
	List<AdminSellerOrderResponse> sellerOrderRows = order.getSellerOrders().stream()
			.sorted(Comparator.comparing(SellerOrder::getId))
			.map(sellerOrder -> toSellerOrder(sellerOrder, imageUrls))
			.toList();
	List<AdminOrderTimelineEvent> timeline = sellerOrderRows.stream()
			.flatMap(sellerOrder -> sellerOrder.history().stream().map(event -> {
				String label = event.changedBy().equalsIgnoreCase("PAYMENT")
						&& event.status() == OrderStatus.PLACED ? "Payment successful" : label(event.status());
				return new AdminOrderTimelineEvent(label, event.status(), event.changedAt(), sellerOrder.sellerName(), event.note());
			}))
			.sorted(Comparator.comparing(AdminOrderTimelineEvent::timestamp))
			.toList();
		ShippingAddressResponse address = new ShippingAddressResponse(
				order.getShippingFullName(), order.getShippingMobileNumber(), order.getShippingHouseFlatNumber(),
				order.getShippingStreetArea(), order.getShippingLandmark(), order.getShippingCity(),
				order.getShippingState(), order.getShippingPinCode(), order.getShippingAddressType());
		return new AdminOrderResponse(
				order.getId(), order.getCustomer().getId(), order.getCustomer().getFullName(),
				order.getCustomer().getEmail(), order.getCustomer().getPhone(), order.getOrderStatus(),
				order.getPaymentMethod(), order.getPaymentStatus(), order.getRefundStatus(), order.getSubtotal(),
				order.getDeliveryCharges(), order.getTotalAmount(), address, sellerOrderRows, payment,
				timeline, order.isCancellationEligible(), order.getCreatedAt());
	}

	private AdminSellerOrderResponse toSellerOrder(SellerOrder sellerOrder, Map<Long, String> imageUrls) {
	List<AdminOrderItemResponse> items = sellerOrder.getItems().stream()
			.map(item -> toItem(item, sellerOrder.getSellerName(), imageUrls))
			.toList();
	List<OrderStatusHistoryResponse> history = sellerOrder.getStatusHistory().stream()
			.map(event -> new OrderStatusHistoryResponse(event.getStatus(), event.getChangedAt(), event.getChangedBy(), event.getNote()))
			.toList();
	return new AdminSellerOrderResponse(
			sellerOrder.getId(), sellerOrder.getSeller().getId(), sellerOrder.getSellerName(),
			sellerOrder.getOrderStatus(), sellerOrder.getSubtotal(), sellerOrder.getDeliveryCharge(),
			sellerOrder.getTotalAmount(), items, history);
	}

	private AdminOrderItemResponse toItem(OrderItem item, String sellerName, Map<Long, String> imageUrls) {
		Long productId = item.getProduct().getId();
		return new AdminOrderItemResponse(productId, item.getProductName(), item.getSku(),
				imageUrls.get(productId), sellerName, item.getQuantity(), item.getUnitPrice(), item.getLineSubtotal());
	}

	private Specification<Order> filter(
			String rawSearch, OrderStatus orderStatus,
			com.shopsphere.common.enums.PaymentStatus paymentStatus, OrderStatus deliveryStatus,
			LocalDate from, LocalDate to) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (orderStatus != null) predicates.add(builder.equal(root.get("orderStatus"), orderStatus));
			if (paymentStatus != null) predicates.add(builder.equal(root.get("paymentStatus"), paymentStatus));
			if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay(MARKETPLACE_ZONE).toInstant()));
			if (to != null) predicates.add(builder.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay(MARKETPLACE_ZONE).toInstant()));
			if (rawSearch != null && !rawSearch.isBlank()) {
				String search = "%" + escapeLike(rawSearch.trim().toLowerCase(Locale.ROOT)) + "%";
				var customer = root.join("customer");
				List<Predicate> searchPredicates = new ArrayList<>();
				searchPredicates.add(builder.like(builder.lower(customer.get("fullName")), search, '\\'));
				searchPredicates.add(builder.like(builder.lower(customer.get("email")), search, '\\'));
				String digits = rawSearch.replaceAll("\\D", "");
				if (!digits.isBlank()) {
					try { searchPredicates.add(builder.equal(root.get("id"), Long.valueOf(digits))); }
					catch (NumberFormatException ignored) { }
				}
				predicates.add(builder.or(searchPredicates.toArray(Predicate[]::new)));
			}
			if (deliveryStatus != null) {
				Subquery<Long> deliveryQuery = query.subquery(Long.class);
				var sellerOrder = deliveryQuery.from(SellerOrder.class);
				deliveryQuery.select(sellerOrder.get("id"))
						.where(builder.equal(sellerOrder.get("order").get("id"), root.get("id")),
								builder.equal(sellerOrder.get("orderStatus"), deliveryStatus));
				predicates.add(builder.exists(deliveryQuery));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private void validateDates(LocalDate from, LocalDate to) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date cannot be after end date.");
		}
	}

	private String label(OrderStatus status) {
		String name = status.name().toLowerCase(Locale.ROOT).replace('_', ' ');
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	private String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private String csv(String value) {
		String safe = value == null ? "" : value;
		if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) safe = "'" + safe;
		return "\"" + safe.replace("\"", "\"\"") + "\"";
	}
}

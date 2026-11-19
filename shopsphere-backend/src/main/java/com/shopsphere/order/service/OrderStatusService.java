package com.shopsphere.order.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.time.Instant;
import java.util.Locale;
import java.util.HashMap;

import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.PaymentMethod;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.order.dto.CancellationResponse;
import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.order.dto.OrderTrackingResponse;
import com.shopsphere.order.dto.SellerOrderTrackingResponse;
import com.shopsphere.order.dto.SellerOrderManagementPageResponse;
import com.shopsphere.order.dto.SellerOrderManagementResponse;
import com.shopsphere.order.dto.SellerOrderSummaryResponse;
import com.shopsphere.order.entity.Order;
import com.shopsphere.order.entity.OrderItem;
import com.shopsphere.order.entity.SellerOrder;
import com.shopsphere.order.mapper.OrderMapper;
import com.shopsphere.order.repository.OrderRepository;
import com.shopsphere.order.repository.SellerOrderRepository;
import com.shopsphere.inventory.service.InventoryService;
import com.shopsphere.payment.entity.Payment;
import com.shopsphere.payment.repository.PaymentRepository;
import com.shopsphere.payment.service.RazorpayGateway;
import com.shopsphere.product.entity.Product;
import com.shopsphere.product.repository.ProductRepository;
import com.shopsphere.product.repository.ProductImageRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderStatusService {

	@PersistenceContext
	private EntityManager entityManager;

	private final OrderRepository orders;
	private final SellerOrderRepository sellerOrders;
	private final ProductRepository products;
	private final PaymentRepository payments;
	private final RazorpayGateway razorpay;
	private final InventoryService inventory;
	private final ProductImageRepository productImages;

	public OrderStatusService(
			OrderRepository orders,
			SellerOrderRepository sellerOrders,
			ProductRepository products,
			PaymentRepository payments,
			RazorpayGateway razorpay,
			InventoryService inventory,
			ProductImageRepository productImages) {
		this.orders = orders;
		this.sellerOrders = sellerOrders;
		this.products = products;
		this.payments = payments;
		this.razorpay = razorpay;
		this.inventory = inventory;
		this.productImages = productImages;
	}

	@Transactional(readOnly = true)
	public SellerOrderManagementPageResponse listSellerOrders(Long sellerUserId, int page, int size, String search,
			String status, Instant from, Instant to) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order start date must be before end date.");
		}
		Specification<SellerOrder> specification = (root, query, cb) -> {
			List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("seller").get("user").get("id"), sellerUserId));
			if (search != null && !search.isBlank()) {
				String term = search.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
				String pattern = "%" + term + "%";
				List<jakarta.persistence.criteria.Predicate> searchPredicates = new ArrayList<>();
				searchPredicates.add(cb.like(cb.lower(root.get("order").get("customer").get("fullName")), pattern, '\\'));
				searchPredicates.add(cb.like(cb.lower(root.get("order").get("customer").get("email")), pattern, '\\'));
				String orderIdTerm = term.replaceFirst("^#?ord-", "").replaceFirst("^#", "");
				try { searchPredicates.add(cb.equal(root.get("order").get("id"), Long.parseLong(orderIdTerm))); }
				catch (NumberFormatException ignored) { /* Name and email remain searchable. */ }
				predicates.add(cb.or(searchPredicates.toArray(jakarta.persistence.criteria.Predicate[]::new)));
			}
			applySellerOrderStatus(predicates, cb, root, status);
			if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("order").<Instant>get("createdAt"), from));
			if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("order").<Instant>get("createdAt"), to));
			return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
		Page<SellerOrder> result = sellerOrders.findAll(specification,
				PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
		List<Long> productIds = result.getContent().stream().flatMap(sellerOrder -> sellerOrder.getItems().stream())
				.map(item -> item.getProduct().getId()).distinct().toList();
		Map<Long, String> imageUrls = new HashMap<>();
		if (!productIds.isEmpty()) {
			productImages.findByProduct_IdInOrderByDisplayOrderAscIdAsc(productIds).forEach(image -> {
				if (image.isPrimary() || !imageUrls.containsKey(image.getProduct().getId())) imageUrls.put(image.getProduct().getId(), image.getUrl());
			});
		}
		return SellerOrderManagementPageResponse.from(result.map(sellerOrder -> SellerOrderManagementResponse.from(sellerOrder, imageUrls)));
	}

	@Transactional(readOnly = true)
	public SellerOrderManagementResponse getSellerOrderDetails(Long sellerUserId, Long sellerOrderId) {
		SellerOrder sellerOrder = sellerOrders.findWithDetailsByIdAndSeller_User_Id(sellerOrderId, sellerUserId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller order was not found."));
		List<Long> productIds = sellerOrder.getItems().stream().map(item -> item.getProduct().getId()).distinct().toList();
		Map<Long, String> imageUrls = new HashMap<>();
		if (!productIds.isEmpty()) {
			productImages.findByProduct_IdInOrderByDisplayOrderAscIdAsc(productIds).forEach(image -> {
				if (image.isPrimary() || !imageUrls.containsKey(image.getProduct().getId())) imageUrls.put(image.getProduct().getId(), image.getUrl());
			});
		}
		return SellerOrderManagementResponse.from(sellerOrder, imageUrls);
	}

	@Transactional(readOnly = true)
	public SellerOrderSummaryResponse sellerOrderSummary(Long sellerUserId) {
		long newOrders = sellerOrders.countBySeller_User_IdAndOrderStatus(sellerUserId, OrderStatus.PLACED)
				+ sellerOrders.countBySeller_User_IdAndOrderStatus(sellerUserId, OrderStatus.CONFIRMED);
		long shipped = sellerOrders.countBySeller_User_IdAndOrderStatus(sellerUserId, OrderStatus.SHIPPED)
				+ sellerOrders.countBySeller_User_IdAndOrderStatus(sellerUserId, OrderStatus.OUT_FOR_DELIVERY);
		return new SellerOrderSummaryResponse(sellerOrders.countBySeller_User_Id(sellerUserId), newOrders,
				sellerOrders.countBySeller_User_IdAndOrderStatus(sellerUserId, OrderStatus.PROCESSING), shipped,
				sellerOrders.countBySeller_User_IdAndOrderStatus(sellerUserId, OrderStatus.DELIVERED),
				sellerOrders.countBySeller_User_IdAndOrderStatus(sellerUserId, OrderStatus.CANCELLED));
	}

	private void applySellerOrderStatus(List<jakarta.persistence.criteria.Predicate> predicates,
			jakarta.persistence.criteria.CriteriaBuilder cb, jakarta.persistence.criteria.Root<SellerOrder> root, String status) {
		if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return;
		switch (status.trim().toUpperCase(Locale.ROOT)) {
			case "NEW" -> predicates.add(root.get("orderStatus").in(OrderStatus.PLACED, OrderStatus.CONFIRMED));
			case "PROCESSING" -> predicates.add(cb.equal(root.get("orderStatus"), OrderStatus.PROCESSING));
			case "SHIPPED" -> predicates.add(root.get("orderStatus").in(OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY));
			case "DELIVERED" -> predicates.add(cb.equal(root.get("orderStatus"), OrderStatus.DELIVERED));
			case "CANCELLED" -> predicates.add(cb.equal(root.get("orderStatus"), OrderStatus.CANCELLED));
			default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order status filter is invalid.");
		}
	}

	@Transactional(readOnly = true)
	public OrderTrackingResponse getTracking(Long customerId, Long orderId) {
		Order order = orders.findTrackingByIdAndCustomer_Id(orderId, customerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order was not found."));
		return OrderMapper.toTrackingResponse(order);
	}

	@Transactional
	public SellerOrderTrackingResponse updateSellerStatus(Long sellerUserId, Long sellerOrderId, OrderStatus nextStatus) {
		SellerOrder sellerOrder = sellerOrders.findByIdAndSeller_User_Id(sellerOrderId, sellerUserId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller order was not found."));
		Order order = orders.findByIdForUpdate(sellerOrder.getOrder().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order was not found."));
		entityManager.refresh(sellerOrder);
		if (order.getPaymentMethod() == PaymentMethod.ONLINE
				&& order.getPaymentStatus() != PaymentStatus.SUCCESS) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "The order cannot be fulfilled before payment.");
		}
		if (nextStatus == OrderStatus.PAYMENT_PENDING || nextStatus == OrderStatus.CANCELLED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This status cannot be set by a seller.");
		}
		try {
			sellerOrder.transitionTo(nextStatus, "SELLER", "Order status updated by the seller.");
		} catch (IllegalStateException exception) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This order status transition is not allowed.");
		}
		order.refreshOrderStatus();
		return OrderMapper.toSellerOrderTrackingResponse(sellerOrder);
	}

	@Transactional
	public CancellationResponse cancelCustomerOrder(Long customerId, Long orderId) {
		Order order = orders.findByIdAndCustomer_IdForUpdate(orderId, customerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order was not found."));
		if (order.getOrderStatus() == OrderStatus.CANCELLED) {
			boolean refundPending = order.getPaymentMethod() == PaymentMethod.ONLINE
					&& order.getPaymentStatus() == PaymentStatus.SUCCESS
					&& requestRefund(order);
			return new CancellationResponse(
					OrderMapper.toResponse(order),
					refundPending);
		}
		if (!order.isCancellationEligible()) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"Orders can only be cancelled before seller processing begins.");
		}

		Map<Long, Integer> quantitiesByProduct = quantitiesByProduct(order);
		restoreStock(quantitiesByProduct);

		boolean refundPending = order.getPaymentMethod() == PaymentMethod.ONLINE
				&& order.getPaymentStatus() == PaymentStatus.SUCCESS
				&& requestRefund(order);

		try {
			order.cancel("CUSTOMER", "Cancelled by the customer.");
		} catch (IllegalStateException exception) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This order can no longer be cancelled.");
		}
		return new CancellationResponse(OrderMapper.toResponse(order), refundPending);
	}

	@Transactional
	public CancellationResponse cancelAdminOrder(Long orderId, String reason) {
		Order order = orders.findByIdForUpdate(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order was not found."));
		if (!order.isCancellationEligible()) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"This order can no longer be cancelled because seller fulfillment has started or completed.");
		}
		restoreStock(quantitiesByProduct(order));
		boolean refundPending = order.getPaymentMethod() == PaymentMethod.ONLINE
				&& order.getPaymentStatus() == PaymentStatus.SUCCESS
				&& requestRefund(order);
		try {
			order.cancel("ADMIN", "Cancelled by admin: " + reason.trim());
		} catch (IllegalStateException exception) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This order can no longer be cancelled.");
		}
		return new CancellationResponse(OrderMapper.toResponse(order), refundPending);
	}

	private boolean requestRefund(Order order) {
		List<Payment> successfulPayments = payments.findByOrder_IdAndStatusForUpdate(
				order.getId(),
				PaymentStatus.SUCCESS);
		if (successfulPayments.size() != 1) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"Payment refund could not be matched to this order.");
		}
		Payment payment = successfulPayments.get(0);
		if (payment.getRazorpayRefundId() != null) {
			return true;
		}
		RazorpayGateway.GatewayRefund refund = razorpay.refundPayment(
				payment.getRazorpayPaymentId(),
				payment.getAmount().movePointRight(2).longValueExact(),
				"cancel-" + order.getId());
		if ("processed".equalsIgnoreCase(refund.status())
				|| "completed".equalsIgnoreCase(refund.status())) {
			payment.markRefunded(payment.getRazorpayPaymentId(), refund.id());
			order.markPaymentRefunded();
			return false;
		}
		if ("pending".equalsIgnoreCase(refund.status())) {
			payment.markRefundInitiated(refund.id());
			order.markRefundPending();
			return true;
		}
		throw new ResponseStatusException(
				HttpStatus.BAD_GATEWAY,
				"Razorpay could not confirm the refund request.");
	}

	private Map<Long, Integer> quantitiesByProduct(Order order) {
		Map<Long, Integer> quantities = new TreeMap<>();
		for (SellerOrder sellerOrder : order.getSellerOrders()) {
			for (OrderItem item : sellerOrder.getItems()) {
				Long productId = item.getProduct().getId();
				try {
					quantities.merge(productId, item.getQuantity(), Math::addExact);
				} catch (ArithmeticException exception) {
					throw new ResponseStatusException(HttpStatus.CONFLICT, "Order item quantities cannot be restored.");
				}
			}
		}
		return quantities;
	}

	private void restoreStock(Map<Long, Integer> quantitiesByProduct) {
		List<Long> productIds = new ArrayList<>(quantitiesByProduct.keySet());
		List<Product> lockedProducts = products.findAllByIdInForUpdate(productIds);
		if (lockedProducts.size() != productIds.size()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A product in this order can no longer be restored.");
		}
		try {
			for (Product product : lockedProducts) {
				int previousQuantity = product.getStockQuantity();
				product.increaseStock(quantitiesByProduct.get(product.getId()));
				inventory.recordChange(product, previousQuantity, product.getStockQuantity(), "ORDER_CANCELLED");
			}
		} catch (ArithmeticException | IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Product stock could not be restored.");
		}
	}
}

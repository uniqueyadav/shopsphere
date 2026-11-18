package com.shopsphere.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.PaymentMethod;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.common.enums.RefundStatus;
import com.shopsphere.order.entity.SellerOrder;

public record SellerOrderManagementResponse(
		Long sellerOrderId,
		Long orderId,
		String customerName,
		String customerEmail,
		OrderStatus status,
		BigDecimal totalAmount,
		Instant orderDate,
		PaymentMethod paymentMethod,
		PaymentStatus paymentStatus,
		RefundStatus refundStatus,
		BigDecimal subtotal,
		BigDecimal discountAmount,
		BigDecimal taxAmount,
		BigDecimal shippingCharges,
		String shippingName,
		String shippingMobile,
		String shippingAddress,
		String shippingMethod,
		Instant estimatedDelivery,
		List<SellerOrderItemManagementResponse> items,
		List<OrderStatusHistoryResponse> history) {

	public static SellerOrderManagementResponse from(SellerOrder sellerOrder, java.util.Map<Long, String> imageUrls) {
		var order = sellerOrder.getOrder();
		List<SellerOrderItemManagementResponse> itemResponses = sellerOrder.getItems().stream().map(item ->
				new SellerOrderItemManagementResponse(item.getProduct().getId(), item.getProductName(), item.getSku(),
						imageUrls.get(item.getProduct().getId()), item.getQuantity(), item.getUnitPrice(), item.getLineSubtotal())).toList();
		String address = String.join(", ", java.util.stream.Stream.of(
				order.getShippingHouseFlatNumber(), order.getShippingStreetArea(), order.getShippingLandmark(),
					order.getShippingCity(), order.getShippingState(), order.getShippingPinCode())
				.filter(value -> value != null && !value.isBlank()).toList());
		return new SellerOrderManagementResponse(sellerOrder.getId(), order.getId(), order.getCustomer().getFullName(),
				order.getCustomer().getEmail(), sellerOrder.getOrderStatus(), sellerOrder.getTotalAmount(),
				order.getCreatedAt(), order.getPaymentMethod(), order.getPaymentStatus(), order.getRefundStatus(),
				sellerOrder.getSubtotal(), order.getSellerOrders().size() == 1 ? order.getDiscountAmount() : null,
				null, sellerOrder.getDeliveryCharge(), order.getShippingFullName(), order.getShippingMobileNumber(), address,
				null, null, itemResponses, sellerOrder.getStatusHistory().stream()
						.map(event -> new OrderStatusHistoryResponse(event.getStatus(), event.getChangedAt(), event.getChangedBy(), event.getNote())).toList());
	}
}

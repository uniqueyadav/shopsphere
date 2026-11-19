package com.shopsphere.order.mapper;

import java.util.List;
import java.util.Map;

import com.shopsphere.order.dto.OrderItemResponse;
import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.order.dto.OrderStatusHistoryResponse;
import com.shopsphere.order.dto.OrderTrackingResponse;
import com.shopsphere.order.dto.SellerOrderResponse;
import com.shopsphere.order.dto.SellerOrderTrackingResponse;
import com.shopsphere.order.dto.ShippingAddressResponse;
import com.shopsphere.order.entity.Order;
import com.shopsphere.order.entity.OrderItem;
import com.shopsphere.order.entity.OrderStatusHistory;
import com.shopsphere.order.entity.SellerOrder;

public final class OrderMapper {

	private OrderMapper() {
	}

	public static OrderResponse toResponse(Order order) {
		return toResponse(order, Map.of());
	}

	public static OrderResponse toResponse(Order order, Map<Long, String> productImages) {
		List<SellerOrderResponse> sellerOrders = order.getSellerOrders().stream()
				.map(sellerOrder -> toSellerOrderResponse(sellerOrder, productImages))
				.toList();
		ShippingAddressResponse address = new ShippingAddressResponse(
				order.getShippingFullName(),
				order.getShippingMobileNumber(),
				order.getShippingHouseFlatNumber(),
				order.getShippingStreetArea(),
				order.getShippingLandmark(),
				order.getShippingCity(),
				order.getShippingState(),
				order.getShippingPinCode(),
				order.getShippingAddressType());
		return new OrderResponse(
				order.getId(),
				order.getOrderStatus(),
				order.getPaymentMethod(),
				order.getPaymentStatus(),
				order.getRefundStatus(),
				order.getSubtotal(),
				order.getDeliveryCharges(),
				order.getDiscountAmount(),
				order.getCouponCode(),
				order.getTotalAmount(),
				address,
				sellerOrders,
				order.getCreatedAt());
	}

	public static OrderTrackingResponse toTrackingResponse(Order order) {
		List<SellerOrderTrackingResponse> sellerOrders = order.getSellerOrders().stream()
				.map(OrderMapper::toSellerOrderTrackingResponse)
				.toList();
		return new OrderTrackingResponse(
				order.getId(),
				order.getOrderStatus(),
				order.getPaymentStatus(),
				order.getRefundStatus(),
				order.getCreatedAt(),
				sellerOrders);
	}

	public static SellerOrderTrackingResponse toSellerOrderTrackingResponse(SellerOrder sellerOrder) {
		return new SellerOrderTrackingResponse(
				sellerOrder.getId(),
				sellerOrder.getSeller().getId(),
				sellerOrder.getSellerName(),
				sellerOrder.getOrderStatus(),
				sellerOrder.getStatusHistory().stream()
						.map(OrderMapper::toStatusHistoryResponse)
						.toList());
	}

	private static OrderStatusHistoryResponse toStatusHistoryResponse(OrderStatusHistory history) {
		return new OrderStatusHistoryResponse(
				history.getStatus(),
				history.getChangedAt(),
				history.getChangedBy(),
				history.getNote(),
				history.getLocationLatitude(),
				history.getLocationLongitude());
	}

	private static SellerOrderResponse toSellerOrderResponse(SellerOrder sellerOrder, Map<Long, String> productImages) {
		List<OrderItemResponse> items = sellerOrder.getItems().stream()
				.map(item -> toOrderItemResponse(item, productImages))
				.toList();
		return new SellerOrderResponse(
				sellerOrder.getId(),
				sellerOrder.getSeller().getId(),
				sellerOrder.getSellerName(),
				sellerOrder.getOrderStatus(),
				sellerOrder.getSubtotal(),
				sellerOrder.getDeliveryCharge(),
				sellerOrder.getTotalAmount(),
				items);
	}

	private static OrderItemResponse toOrderItemResponse(OrderItem item, Map<Long, String> productImages) {
		return new OrderItemResponse(
				item.getProduct().getId(),
				item.getProductName(),
				productImages.get(item.getProduct().getId()),
				item.getSku(),
				item.getQuantity(),
				item.getUnitPrice(),
				item.getLineSubtotal());
	}
}

package com.shopsphere.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.PaymentMethod;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.common.enums.RefundStatus;

public record OrderResponse(
		Long id,
		OrderStatus orderStatus,
		PaymentMethod paymentMethod,
		PaymentStatus paymentStatus,
		RefundStatus refundStatus,
		BigDecimal subtotal,
		BigDecimal deliveryCharges,
		BigDecimal discountAmount,
		String couponCode,
		BigDecimal totalAmount,
		ShippingAddressResponse shippingAddress,
		List<SellerOrderResponse> sellerOrders,
		Instant createdAt) {
}

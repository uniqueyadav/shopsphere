package com.shopsphere.seller.dto;

import java.math.BigDecimal;

public record SellerDeliveryFeeResponse(Long sellerId, String storeName, BigDecimal deliveryFee) {
}

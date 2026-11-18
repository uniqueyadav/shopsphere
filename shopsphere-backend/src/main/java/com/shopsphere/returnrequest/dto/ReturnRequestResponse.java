package com.shopsphere.returnrequest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.shopsphere.common.enums.ReturnRequestStatus;
import com.shopsphere.common.enums.ReturnRequestType;

public record ReturnRequestResponse(
		Long id, Long orderId, Long orderItemId, String productName, String imageUrl, String sku, String sellerName,
		int quantity, BigDecimal itemAmount, ReturnRequestType requestType, String reason, String description,
		ReturnRequestStatus status, BigDecimal refundAmount, Instant requestedAt, Instant expectedPickupAt,
		String resolutionNote, List<ReturnRequestHistoryResponse> history) { }

package com.shopsphere.refund.dto;

import java.math.BigDecimal;

public record AdminRefundStatsResponse(long totalCount, BigDecimal totalAmount, long pendingCount, BigDecimal pendingAmount, long refundedCount, BigDecimal refundedAmount, long failedCount, BigDecimal failedAmount) { }

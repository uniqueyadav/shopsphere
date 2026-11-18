package com.shopsphere.refund.repository;

import java.math.BigDecimal;

import com.shopsphere.common.enums.RefundStatus;

public interface AdminRefundAggregate {
	RefundStatus getStatus();
	Long getTotalCount();
	BigDecimal getTotalAmount();
}

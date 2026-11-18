package com.shopsphere.payment.repository;

import java.math.BigDecimal;

import com.shopsphere.common.enums.PaymentStatus;

public interface AdminPaymentAggregate {
	PaymentStatus getStatus();
	Long getTotalCount();
	BigDecimal getTotalAmount();
}

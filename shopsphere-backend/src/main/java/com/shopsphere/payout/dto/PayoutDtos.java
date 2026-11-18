package com.shopsphere.payout.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.shopsphere.payout.entity.PayoutMethod;
import com.shopsphere.payout.entity.PayoutStatus;

public final class PayoutDtos {
	private PayoutDtos() {}
	public record Summary(BigDecimal availableBalance, BigDecimal pendingPayout, BigDecimal totalPayouts,
			BigDecimal thisMonthEarnings, PayoutAccount account, List<Transaction> transactions,
			int minimumWithdrawal, int cooldownDays, long totalElements, int totalPages, int page, int size) {}
	public record PayoutAccount(PayoutMethod method, String accountHolder, String maskedDestination) {}
	public record Transaction(Long id, Instant date, String type, BigDecimal amount, String status,
			String transactionId, String remarks, Long relatedOrderId) {}
	public record AdminPayout(Long id, Long sellerId, String storeName, BigDecimal amount,
			PayoutStatus status, PayoutMethod method, String accountHolder, String maskedDestination,
			String destination, String transferReference, String remarks, Instant requestedAt) {}
}

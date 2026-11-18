package com.shopsphere.commission.dto;

import java.math.BigDecimal;
import java.time.Instant;
import com.shopsphere.commission.entity.CommissionRule;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CommissionDtos {
	private CommissionDtos(){}
	public record UpdateRequest(@NotNull @DecimalMin("0.00") @DecimalMax("50.00") BigDecimal ratePercent,@NotNull Instant effectiveFrom,Instant effectiveTo,@Size(max=500) String reason){}
	public record RuleResponse(Long id,CommissionRule.Scope scope,Long categoryId,String categoryName,BigDecimal ratePercent,Instant effectiveFrom,Instant effectiveTo,boolean active,String reason,String updatedBy,Instant updatedAt){}
	public record PreviewResponse(BigDecimal orderAmount,BigDecimal ratePercent,BigDecimal platformCommission,BigDecimal sellerAmount){}
}

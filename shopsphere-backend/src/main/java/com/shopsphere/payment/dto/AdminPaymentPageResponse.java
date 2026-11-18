package com.shopsphere.payment.dto;

import java.util.List;

public record AdminPaymentPageResponse(List<AdminPaymentResponse> items, int page, int size, long totalElements, int totalPages) {
}

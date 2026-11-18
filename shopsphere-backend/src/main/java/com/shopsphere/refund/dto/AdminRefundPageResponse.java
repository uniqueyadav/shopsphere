package com.shopsphere.refund.dto;

import java.util.List;

public record AdminRefundPageResponse(List<AdminRefundResponse> items, int page, int size, long totalElements, int totalPages) { }

package com.shopsphere.returnrequest.dto;

import java.time.Instant;
import com.shopsphere.common.enums.ReturnRequestStatus;

public record ReturnRequestHistoryResponse(ReturnRequestStatus status, Instant changedAt, String note) { }

package com.shopsphere.returnrequest.dto;

import java.time.Instant;
import com.shopsphere.common.enums.ReturnRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateReturnRequestStatus(@NotNull ReturnRequestStatus status, @Size(max = 500) String note, Instant expectedPickupAt) { }

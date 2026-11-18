package com.shopsphere.returnrequest.dto;

import com.shopsphere.common.enums.ReturnRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateReturnRequest(
		@NotNull @Positive Long orderItemId,
		@NotNull ReturnRequestType requestType,
		@NotBlank @Size(max = 60) String reason,
		@Size(max = 1000) String description) { }

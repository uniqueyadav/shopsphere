package com.shopsphere.admin.dto;

import com.shopsphere.common.enums.AccountStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull AccountStatus accountStatus) {
}

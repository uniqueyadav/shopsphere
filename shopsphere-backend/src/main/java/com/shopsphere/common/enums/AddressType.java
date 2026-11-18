package com.shopsphere.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AddressType {
	HOME,
	WORK,
	OTHER;

	@JsonCreator
	public static AddressType fromValue(String value) {
		if (value == null) {
			throw new IllegalArgumentException("Address type is required.");
		}
		String normalized = value.trim();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("Address type is required.");
		}
		for (AddressType type : values()) {
			if (type.name().equalsIgnoreCase(normalized)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unsupported address type: " + value);
	}

	@JsonValue
	public String toJsonValue() {
		return name();
	}
}

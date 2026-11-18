package com.shopsphere.user.entity;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.common.enums.AddressType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "addresses")
public class Address extends BaseEntity {

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_addresses_user"))
	private User user;

	@Column(nullable = false, length = 100)
	private String fullName;

	@Column(name = "mobile_number", nullable = false, length = 20)
	private String mobileNumber;

	@Column(name = "house_flat_number", nullable = false, length = 100)
	private String houseFlatNumber;

	@Column(name = "street_area", nullable = false, length = 255)
	private String streetArea;

	@Column(length = 150)
	private String landmark;

	@Column(nullable = false, length = 100)
	private String city;

	@Column(nullable = false, length = 100)
	private String state;

	@Column(name = "pin_code", nullable = false, length = 20)
	private String pinCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "address_type", nullable = false, length = 10)
	private AddressType addressType;

	@Column(name = "default_address", nullable = false)
	private boolean defaultAddress;

	@Column(name = "delivery_instructions", length = 500)
	private String deliveryInstructions;

	@Column(name = "leave_package_at_door", nullable = false)
	private boolean leavePackageAtDoor;

	@Column(name = "call_before_delivery", nullable = false)
	private boolean callBeforeDelivery;

	protected Address() {
	}

	public Address(
			User user,
			String fullName,
			String mobileNumber,
			String houseFlatNumber,
			String streetArea,
			String landmark,
			String city,
			String state,
			String pinCode,
			AddressType addressType,
			boolean defaultAddress) {
		this(user, fullName, mobileNumber, houseFlatNumber, streetArea, landmark, city, state, pinCode,
				addressType, defaultAddress, null, false, false);
	}

	public Address(
			User user,
			String fullName,
			String mobileNumber,
			String houseFlatNumber,
			String streetArea,
			String landmark,
			String city,
			String state,
			String pinCode,
			AddressType addressType,
			boolean defaultAddress,
			String deliveryInstructions,
			boolean leavePackageAtDoor,
			boolean callBeforeDelivery) {
		this.user = user;
		update(fullName, mobileNumber, houseFlatNumber, streetArea, landmark, city, state, pinCode,
				addressType, deliveryInstructions, leavePackageAtDoor, callBeforeDelivery);
		this.defaultAddress = defaultAddress;
	}

	public User getUser() {
		return user;
	}

	public String getFullName() {
		return fullName;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public String getHouseFlatNumber() {
		return houseFlatNumber;
	}

	public String getStreetArea() {
		return streetArea;
	}

	public String getLandmark() {
		return landmark;
	}

	public String getCity() {
		return city;
	}

	public String getState() {
		return state;
	}

	public String getPinCode() {
		return pinCode;
	}

	public AddressType getAddressType() {
		return addressType;
	}

	public boolean isDefaultAddress() {
		return defaultAddress;
	}

	public String getDeliveryInstructions() { return deliveryInstructions; }
	public boolean isLeavePackageAtDoor() { return leavePackageAtDoor; }
	public boolean isCallBeforeDelivery() { return callBeforeDelivery; }

	public void update(
			String fullName,
			String mobileNumber,
			String houseFlatNumber,
			String streetArea,
			String landmark,
			String city,
			String state,
			String pinCode,
			AddressType addressType) {
		update(fullName, mobileNumber, houseFlatNumber, streetArea, landmark, city, state, pinCode,
				addressType, deliveryInstructions, leavePackageAtDoor, callBeforeDelivery);
	}

	public void update(
			String fullName,
			String mobileNumber,
			String houseFlatNumber,
			String streetArea,
			String landmark,
			String city,
			String state,
			String pinCode,
			AddressType addressType,
			String deliveryInstructions,
			boolean leavePackageAtDoor,
			boolean callBeforeDelivery) {
		this.fullName = fullName;
		this.mobileNumber = mobileNumber;
		this.houseFlatNumber = houseFlatNumber;
		this.streetArea = streetArea;
		this.landmark = landmark;
		this.city = city;
		this.state = state;
		this.pinCode = pinCode;
		this.addressType = addressType;
		this.deliveryInstructions = deliveryInstructions;
		this.leavePackageAtDoor = leavePackageAtDoor;
		this.callBeforeDelivery = callBeforeDelivery;
	}

	public void setDefaultAddress(boolean defaultAddress) {
		this.defaultAddress = defaultAddress;
	}
}

package com.shopsphere.seller.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.common.enums.BusinessType;
import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sellers")
public class Seller extends BaseEntity {

	@NotNull
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_sellers_user"))
	private User user;

	@NotBlank
	@Size(max = 150)
	@Column(nullable = false, length = 150)
	private String storeName;

	@Size(max = 2000)
	@Column(length = 2000)
	private String storeDescription;

	@Column(name = "seller_full_name", length = 100)
	private String sellerFullName;

	@Column(name = "seller_email", length = 254)
	private String sellerEmail;

	@Column(name = "seller_phone", length = 20)
	private String sellerPhone;

	@Enumerated(EnumType.STRING)
	@Column(name = "business_type", length = 40)
	private BusinessType businessType;

	@Column(name = "gst_number", length = 15)
	private String gstNumber;

	@NotBlank
	@Size(max = 1000)
	@Column(nullable = false, length = 1000)
	private String businessAddress;

	@Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2)
	private BigDecimal deliveryFee = BigDecimal.ZERO;
	@Column(name = "pickup_latitude") private Double pickupLatitude;
	@Column(name = "pickup_longitude") private Double pickupLongitude;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SellerStatus approvalStatus = SellerStatus.PENDING;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reviewed_by", foreignKey = @ForeignKey(name = "fk_sellers_reviewed_by"))
	private User reviewedBy;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@Size(max = 1000)
	@Column(name = "rejection_reason", length = 1000)
	private String rejectionReason;

	@Column(name = "suspension_reason", length = 1000)
	private String suspensionReason;

	@OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SellerDocument> documents = new ArrayList<>();

	protected Seller() {
	}

	public Seller(User user, String storeName, String storeDescription, String businessAddress) {
		this(user, storeName, null, null, null, null, null, storeDescription, businessAddress);
	}

	public Seller(
			User user,
			String storeName,
			String sellerFullName,
			String sellerEmail,
			String sellerPhone,
			BusinessType businessType,
			String gstNumber,
			String storeDescription,
			String businessAddress) {
		this.user = user;
		this.storeName = storeName;
		this.sellerFullName = sellerFullName;
		this.sellerEmail = sellerEmail;
		this.sellerPhone = sellerPhone;
		this.businessType = businessType;
		this.gstNumber = gstNumber;
		this.storeDescription = storeDescription;
		this.businessAddress = businessAddress;
	}

	public User getUser() {
		return user;
	}

	public String getStoreName() {
		return storeName;
	}

	public String getSellerFullName() {
		return sellerFullName;
	}

	public String getSellerEmail() {
		return sellerEmail;
	}

	public String getSellerPhone() {
		return sellerPhone;
	}

	public BusinessType getBusinessType() {
		return businessType;
	}

	public String getGstNumber() {
		return gstNumber;
	}

	public String getStoreDescription() {
		return storeDescription;
	}

	public String getBusinessAddress() {
		return businessAddress;
	}

	public BigDecimal getDeliveryFee() {
		return deliveryFee;
	}
	public Double getPickupLatitude() { return pickupLatitude; }
	public Double getPickupLongitude() { return pickupLongitude; }
	public void updatePickupLocation(Double latitude, Double longitude) { this.pickupLatitude = latitude; this.pickupLongitude = longitude; }

	public void updateDeliveryFee(BigDecimal deliveryFee) {
		this.deliveryFee = deliveryFee;
	}

	public SellerStatus getApprovalStatus() {
		return approvalStatus;
	}

	public void setApprovalStatus(SellerStatus approvalStatus) {
		this.approvalStatus = approvalStatus;
	}

	public User getReviewedBy() {
		return reviewedBy;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}

	public String getRejectionReason() {
		return rejectionReason;
	}

	public String getSuspensionReason() {
		return suspensionReason;
	}

	public List<SellerDocument> getDocuments() {
		return documents;
	}

	public void addDocument(SellerDocument document) {
		documents.add(document);
		document.setSeller(this);
	}

	public void updateProfile(
			String storeName,
			String sellerFullName,
			String sellerEmail,
			String sellerPhone,
			BusinessType businessType,
			String gstNumber,
			String storeDescription,
			String businessAddress) {
		this.storeName = storeName;
		this.sellerFullName = sellerFullName;
		this.sellerEmail = sellerEmail;
		this.sellerPhone = sellerPhone;
		this.businessType = businessType;
		this.gstNumber = gstNumber;
		this.storeDescription = storeDescription;
		this.businessAddress = businessAddress;
	}

	public void recordSuspension(String reason) {
		this.suspensionReason = reason;
	}

	public void recordReview(SellerStatus status, User reviewer, Instant reviewTime, String reason) {
		this.approvalStatus = status;
		this.reviewedBy = reviewer;
		this.reviewedAt = reviewTime;
		this.rejectionReason = reason;
	}
}

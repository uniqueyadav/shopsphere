package com.shopsphere.review.entity;

import java.time.Instant;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.common.enums.ReviewStatus;
import com.shopsphere.product.entity.Product;
import com.shopsphere.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "product_reviews", uniqueConstraints = @UniqueConstraint(name = "uk_review_customer_product", columnNames = {"product_id", "customer_id"}), indexes = {@Index(name = "idx_reviews_status_created", columnList = "status,created_at"), @Index(name = "idx_reviews_product", columnList = "product_id")})
public class Review extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reviews_product"))
	private Product product;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reviews_customer"))
	private User customer;
	@Min(1) @Max(5)
	@Column(nullable = false)
	private int rating;
	@NotBlank
	@Column(name = "review_text", nullable = false, columnDefinition = "TEXT")
	private String reviewText;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
	private ReviewStatus status = ReviewStatus.PENDING;
	@Column(name = "admin_note", length = 1000)
	private String adminNote;
	@Column(name = "rejection_reason", length = 500)
	private String rejectionReason;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "moderated_by", foreignKey = @ForeignKey(name = "fk_reviews_moderated_by"))
	private User moderatedBy;
	@Column(name = "moderated_at")
	private Instant moderatedAt;
	protected Review() {}
	public Review(Product product, User customer, int rating, String reviewText) { this.product=product; this.customer=customer; this.rating=rating; this.reviewText=reviewText; }
	public Product getProduct(){return product;} public User getCustomer(){return customer;} public int getRating(){return rating;} public String getReviewText(){return reviewText;} public ReviewStatus getStatus(){return status;} public String getAdminNote(){return adminNote;} public String getRejectionReason(){return rejectionReason;} public User getModeratedBy(){return moderatedBy;} public Instant getModeratedAt(){return moderatedAt;}
	public void moderate(ReviewStatus status, String note, String reason, User admin) { this.status=status; this.adminNote=note; this.rejectionReason=reason; this.moderatedBy=admin; this.moderatedAt=Instant.now(); }
}

package com.shopsphere.returnrequest.entity;

import java.time.Instant;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.common.enums.ReturnRequestStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "return_request_history")
public class ReturnRequestHistory extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "return_request_id", nullable = false, foreignKey = @ForeignKey(name = "fk_return_request_history_request"))
	private CustomerReturnRequest request;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ReturnRequestStatus status;

	@Column(name = "changed_at", nullable = false)
	private Instant changedAt;

	@Column(nullable = false, length = 20)
	private String actor;

	@Column(length = 500)
	private String note;

	protected ReturnRequestHistory() { }

	public ReturnRequestHistory(CustomerReturnRequest request, ReturnRequestStatus status, String actor, String note) {
		this.request = request;
		this.status = status;
		this.changedAt = Instant.now();
		this.actor = actor;
		this.note = note;
	}

	public ReturnRequestStatus getStatus() { return status; }
	public Instant getChangedAt() { return changedAt; }
	public String getActor() { return actor; }
	public String getNote() { return note; }
}

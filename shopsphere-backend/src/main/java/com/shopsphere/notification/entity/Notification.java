package com.shopsphere.notification.entity;

import java.time.Instant;
import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.user.entity.User;
import jakarta.persistence.*;

@Entity @Table(name="admin_notifications",indexes=@Index(name="idx_admin_notification_created",columnList="created_at"))
public class Notification extends BaseEntity {
	public enum Type { PROMOTIONAL, TRANSACTIONAL, ORDER_UPDATE, PAYMENT, ACCOUNT, SELLER, DELIVERY, SYSTEM, INFORMATIONAL }
	public enum Audience { ALL_USERS, CUSTOMERS, SELLERS, DELIVERY_PARTNERS }
	public enum Status { DRAFT, SCHEDULED, QUEUED, SENT, FAILED, CANCELLED }
	@Column(nullable=false,length=160) private String title;
	@Column(nullable=false,length=1000) private String message;
	@Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Type type;
	@Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Audience audience;
	@Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status;
	@Column(name="scheduled_at") private Instant scheduledAt;
	@Column(name="recipient_count",nullable=false) private long recipientCount;
	@Column(name="created_by",nullable=false) private Long createdBy;
	@Column(name="dispatched_at") private Instant dispatchedAt;
	protected Notification(){}
	public Notification(String title,String message,Type type,Audience audience,Status status,Instant scheduledAt,long recipientCount,Long createdBy){this.title=title;this.message=message;this.type=type;this.audience=audience;this.status=status;this.scheduledAt=scheduledAt;this.recipientCount=recipientCount;this.createdBy=createdBy;}
	public String getTitle(){return title;} public String getMessage(){return message;} public Type getType(){return type;} public Audience getAudience(){return audience;} public Status getStatus(){return status;} public Instant getScheduledAt(){return scheduledAt;} public long getRecipientCount(){return recipientCount;} public Long getCreatedBy(){return createdBy;} public Instant getDispatchedAt(){return dispatchedAt;}
	public void dispatched(){status=Status.SENT;dispatchedAt=Instant.now();}
	public void fail(){status=Status.FAILED;}
	public void queue(){if(status==Status.SCHEDULED)status=Status.QUEUED;}
}

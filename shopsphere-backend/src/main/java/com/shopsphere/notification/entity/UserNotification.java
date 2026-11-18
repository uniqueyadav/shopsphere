package com.shopsphere.notification.entity;

import com.shopsphere.common.entity.BaseEntity;
import com.shopsphere.user.entity.User;
import jakarta.persistence.*;

@Entity @Table(name="user_notifications",indexes={@Index(name="idx_user_notification_recipient",columnList="recipient_id,created_at"),@Index(name="idx_user_notification_campaign",columnList="campaign_id")})
public class UserNotification extends BaseEntity {
	@ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="recipient_id",nullable=false,foreignKey=@ForeignKey(name="fk_user_notification_recipient")) private User recipient;
	@ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="campaign_id",nullable=false,foreignKey=@ForeignKey(name="fk_user_notification_campaign")) private Notification campaign;
	@Column(nullable=false) private boolean read;
	protected UserNotification(){}
	public UserNotification(User recipient,Notification campaign){this.recipient=recipient;this.campaign=campaign;}
}

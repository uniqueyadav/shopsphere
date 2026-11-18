package com.shopsphere.notification.dto;

import java.time.Instant;
import com.shopsphere.notification.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AdminNotificationDtos {
	private AdminNotificationDtos(){}
	public record CreateRequest(@NotBlank @Size(max=160) String title,@NotBlank @Size(max=1000) String message,@NotNull Notification.Type type,@NotNull Notification.Audience audience,boolean saveAsDraft,Instant scheduledAt){}
	public record Response(Long id,String title,String message,Notification.Type type,Notification.Audience audience,Notification.Status status,Instant createdAt,Instant scheduledAt,long recipientCount,Long createdBy){}
}

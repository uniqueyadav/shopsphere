package com.shopsphere.notification.repository;

import com.shopsphere.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationRepository extends JpaRepository<Notification,Long>,JpaSpecificationExecutor<Notification>{
	long countByStatus(Notification.Status status);
	java.util.List<Notification> findByStatusAndScheduledAtLessThanEqual(Notification.Status status,java.time.Instant time);
}

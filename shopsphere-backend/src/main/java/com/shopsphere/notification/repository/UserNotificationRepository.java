package com.shopsphere.notification.repository;

import com.shopsphere.notification.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationRepository extends JpaRepository<UserNotification,Long>{long countByCampaign_Id(Long campaignId);}

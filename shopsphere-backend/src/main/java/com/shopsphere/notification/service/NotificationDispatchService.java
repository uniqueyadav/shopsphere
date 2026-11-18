package com.shopsphere.notification.service;

import java.util.ArrayList;
import com.shopsphere.common.enums.Role;
import com.shopsphere.notification.entity.Notification;
import com.shopsphere.notification.entity.UserNotification;
import com.shopsphere.notification.repository.NotificationRepository;
import com.shopsphere.notification.repository.NotificationRecipientRepository;
import com.shopsphere.notification.repository.UserNotificationRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationDispatchService {
	private final NotificationRepository campaigns; private final NotificationRecipientRepository users; private final UserNotificationRepository deliveries;
	public NotificationDispatchService(NotificationRepository campaigns,NotificationRecipientRepository users,UserNotificationRepository deliveries){this.campaigns=campaigns;this.users=users;this.deliveries=deliveries;}
	@Async @Transactional public void deliver(Long id){var campaign=campaigns.findById(id).orElse(null);if(campaign==null||campaign.getStatus()!=Notification.Status.QUEUED)return;try{var recipients=switch(campaign.getAudience()){case ALL_USERS->users.findAll().stream().filter(u->u.getRole()!=Role.SUPER_ADMIN).toList();case CUSTOMERS->users.findByRole(Role.CUSTOMER);case SELLERS->users.findByRole(Role.SELLER);case DELIVERY_PARTNERS->users.findByRole(Role.DELIVERY_PARTNER);};for(int start=0;start<recipients.size();start+=500){var batch=new ArrayList<UserNotification>();for(var user:recipients.subList(start,Math.min(start+500,recipients.size())))batch.add(new UserNotification(user,campaign));deliveries.saveAll(batch);}campaign.dispatched();}catch(RuntimeException e){campaign.fail();throw e;}}
	@org.springframework.scheduling.annotation.Scheduled(fixedDelay=30000)
	public void dispatchScheduled(){for(var campaign:campaigns.findByStatusAndScheduledAtLessThanEqual(Notification.Status.SCHEDULED,java.time.Instant.now())){campaign.queue();deliver(campaign.getId());}}
}

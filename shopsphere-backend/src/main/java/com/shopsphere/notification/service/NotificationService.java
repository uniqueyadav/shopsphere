package com.shopsphere.notification.service;

import java.time.Instant;
import java.util.Map;
import com.shopsphere.common.enums.Role;
import com.shopsphere.notification.dto.AdminNotificationDtos.CreateRequest;
import com.shopsphere.notification.dto.AdminNotificationDtos.Response;
import com.shopsphere.notification.entity.Notification;
import com.shopsphere.notification.repository.NotificationRepository;
import com.shopsphere.notification.repository.NotificationRecipientRepository;
import com.shopsphere.notification.repository.UserNotificationRepository;
import com.shopsphere.user.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {
	private final NotificationRepository notifications; private final NotificationRecipientRepository users; private final UserNotificationRepository deliveries; private final NotificationDispatchService dispatch;
	public NotificationService(NotificationRepository notifications,NotificationRecipientRepository users,UserNotificationRepository deliveries,NotificationDispatchService dispatch){this.notifications=notifications;this.users=users;this.deliveries=deliveries;this.dispatch=dispatch;}
	@Transactional(readOnly=true) public Page<Response> list(String search,Notification.Status status,int page,int size){if(page<0||size<1||size>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pagination.");Specification<Notification> spec=(root,q,cb)->{var ps=new java.util.ArrayList<Predicate>();if(status!=null)ps.add(cb.equal(root.get("status"),status));if(search!=null&&!search.isBlank()){String value="%"+search.trim().toLowerCase()+"%";ps.add(cb.or(cb.like(cb.lower(root.get("title")),value),cb.like(cb.lower(root.get("message")),value)));}return cb.and(ps.toArray(Predicate[]::new));};return notifications.findAll(spec,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt"))).map(this::response);}
	@Transactional(readOnly=true) public Response get(Long id){return response(notifications.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found.")));}
	@Transactional(readOnly=true) public Map<String,Long> stats(){return Map.of("totalSent",notifications.countByStatus(Notification.Status.SENT),"delivered",notifications.countByStatus(Notification.Status.SENT),"pending",notifications.countByStatus(Notification.Status.QUEUED)+notifications.countByStatus(Notification.Status.SCHEDULED),"failed",notifications.countByStatus(Notification.Status.FAILED));}
	@Transactional public Response create(CreateRequest req,Long adminId){if(req.scheduledAt()!=null&&!req.scheduledAt().isAfter(Instant.now()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Scheduled time must be in the future.");long count=audience(req.audience()).size();Notification.Status status=req.saveAsDraft()?Notification.Status.DRAFT:req.scheduledAt()!=null?Notification.Status.SCHEDULED:Notification.Status.QUEUED;Notification saved=notifications.save(new Notification(req.title().trim(),req.message().trim(),req.type(),req.audience(),status,req.scheduledAt(),count,adminId));if(status==Notification.Status.QUEUED)dispatch.deliver(saved.getId());return response(saved);}
	private java.util.List<User> audience(Notification.Audience audience){return switch(audience){case ALL_USERS->users.findAll().stream().filter(u->u.getRole()!=Role.SUPER_ADMIN).toList();case CUSTOMERS->users.findByRole(Role.CUSTOMER);case SELLERS->users.findByRole(Role.SELLER);case DELIVERY_PARTNERS->users.findByRole(Role.DELIVERY_PARTNER);};}
	private Response response(Notification n){return new Response(n.getId(),n.getTitle(),n.getMessage(),n.getType(),n.getAudience(),n.getStatus(),n.getCreatedAt(),n.getScheduledAt(),n.getRecipientCount(),n.getCreatedBy());}
}

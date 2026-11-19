package com.shopsphere.delivery.service;

import java.util.Locale;
import java.util.Map;
import com.shopsphere.common.enums.Role;
import com.shopsphere.common.enums.AccountStatus;
import com.shopsphere.delivery.dto.AdminPartnerResponse;
import com.shopsphere.delivery.dto.CreatePartnerRequest;
import com.shopsphere.delivery.entity.DeliveryPartner;
import com.shopsphere.delivery.entity.PartnerStatus;
import com.shopsphere.delivery.dto.PartnerStatusRequest;
import com.shopsphere.delivery.repository.DeliveryPartnerRepository;
import com.shopsphere.user.ShopUserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class DeliveryService {
	private final DeliveryPartnerRepository partners; private final ShopUserRepository users; private final PasswordEncoder passwordEncoder;
	public DeliveryService(DeliveryPartnerRepository partners,ShopUserRepository users,PasswordEncoder passwordEncoder){this.partners=partners;this.users=users;this.passwordEncoder=passwordEncoder;}
	@Transactional(readOnly=true) public Page<AdminPartnerResponse> list(String search,PartnerStatus status,String partnerType,String city,int page,int size){if(page<0||size<1||size>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pagination.");Specification<DeliveryPartner> spec=(root,q,cb)->{var ps=new java.util.ArrayList<Predicate>();if(status!=null)ps.add(cb.equal(root.get("status"),status));if(partnerType!=null&&!partnerType.isBlank())ps.add(cb.equal(root.get("vehicleType").as(String.class),partnerType.toUpperCase(Locale.ROOT)));if(city!=null&&!city.isBlank())ps.add(cb.like(cb.lower(root.get("city")),"%"+city.trim().toLowerCase(Locale.ROOT)+"%"));if(search!=null&&!search.isBlank()){String term="%"+search.trim().toLowerCase(Locale.ROOT)+"%";ps.add(cb.or(cb.like(cb.lower(root.get("user").get("fullName")),term),cb.like(cb.lower(root.get("user").get("email")),term),cb.like(cb.lower(root.get("user").get("phone")),term)));}return cb.and(ps.toArray(Predicate[]::new));};return partners.findAll(spec,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt"))).map(this::response);}
	@Transactional(readOnly=true) public AdminPartnerResponse get(Long id){return response(find(id));}
	@Transactional public AdminPartnerResponse updateServiceCity(Long id,String city){var partner=find(id);partner.updateServiceCity(city);return response(partner);}
	@Transactional(readOnly=true) public Map<String,Long> stats(){return Map.of("totalPartners",partners.count(),"approvedPartners",partners.countByStatus(PartnerStatus.APPROVED),"pendingApproval",partners.countByStatus(PartnerStatus.PENDING),"suspendedPartners",partners.countByStatus(PartnerStatus.SUSPENDED));}
	@Transactional public AdminPartnerResponse create(CreatePartnerRequest request){String email=request.email().trim().toLowerCase(Locale.ROOT);if(users.existsByEmail(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists.");var user=new com.shopsphere.user.entity.User(request.fullName().trim(),email,passwordEncoder.encode(request.password()));if(request.phone()!=null&&!request.phone().isBlank())user.setPhone(request.phone().trim());user.setRole(Role.DELIVERY_PARTNER);user.setAccountStatus(AccountStatus.ACTIVE);user=users.save(user);return response(partners.save(new DeliveryPartner(user,request.vehicleType(),request.city(),request.address(),request.vehicleRegistrationNumber())));}
	@Transactional public AdminPartnerResponse updateStatus(Long id,PartnerStatusRequest request){var p=find(id);switch(request.status()){case APPROVED->p.approve();case REJECTED->{if(request.reason()==null||request.reason().isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A rejection reason is required.");p.reject(request.reason().trim());}case SUSPENDED->{if(request.reason()==null||request.reason().isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A suspension reason is required.");p.suspend(request.reason().trim());}case PENDING->throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cannot move a partner back to pending.");}return response(p);}
	private DeliveryPartner find(Long id){return partners.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Delivery partner not found."));}
	private AdminPartnerResponse response(DeliveryPartner p){var u=p.getUser();return new AdminPartnerResponse(p.getId(),u.getFullName(),u.getEmail(),u.getPhone(),p.getStatus(),p.getVehicleType(),p.getCity(),p.getAddress(),p.getVehicleRegistrationNumber(),p.getRejectionReason(),p.getSuspensionReason(),p.getCreatedAt());}
}

package com.shopsphere.commission.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import com.shopsphere.category.entity.Category;
import com.shopsphere.category.repository.CategoryRepository;
import com.shopsphere.commission.dto.CommissionDtos.*;
import com.shopsphere.commission.entity.CommissionRule;
import com.shopsphere.commission.repository.CommissionRuleRepository;
import com.shopsphere.user.ShopUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommissionService {
	private final CommissionRuleRepository rules; private final CategoryRepository categories; private final ShopUserRepository users;
	public CommissionService(CommissionRuleRepository rules,CategoryRepository categories,ShopUserRepository users){this.rules=rules;this.categories=categories;this.users=users;}
	@Transactional(readOnly=true) public List<RuleResponse> list(){return rules.findAllByOrderByCreatedAtDesc().stream().map(this::dto).toList();}
	@Transactional(readOnly=true) public RuleResponse currentPlatform(){return rules.findFirstByScopeAndCategory_IdIsNullAndEffectiveFromLessThanEqualAndActiveTrueOrderByEffectiveFromDesc(CommissionRule.Scope.PLATFORM,Instant.now()).map(this::dto).orElse(null);}
	@Transactional public RuleResponse updatePlatform(UpdateRequest r,Long adminId){validate(r);var admin=users.findById(adminId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Admin not found."));rules.save(new CommissionRule(CommissionRule.Scope.PLATFORM,null,r.ratePercent(),r.effectiveFrom(),r.effectiveTo(),r.reason(),admin));return currentPlatform();}
	@Transactional public RuleResponse updateCategory(Long categoryId,UpdateRequest r,Long adminId){validate(r);Category category=categories.findById(categoryId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Category not found."));var admin=users.findById(adminId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Admin not found."));return dto(rules.save(new CommissionRule(CommissionRule.Scope.CATEGORY,category,r.ratePercent(),r.effectiveFrom(),r.effectiveTo(),r.reason(),admin)));}
	@Transactional(readOnly=true) public PreviewResponse preview(BigDecimal amount,BigDecimal rate){if(amount==null||amount.signum()<0||rate==null||rate.signum()<0||rate.compareTo(BigDecimal.valueOf(50))>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Amount must be nonnegative and rate must be 0–50%.");BigDecimal commission=amount.multiply(rate).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);return new PreviewResponse(amount,rate,commission,amount.subtract(commission));}
	private void validate(UpdateRequest r){if(r.effectiveTo()!=null&&!r.effectiveTo().isAfter(r.effectiveFrom()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Effective To must be after Effective From.");if(r.effectiveFrom().isBefore(Instant.now().minusSeconds(60)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Effective From cannot be in the past.");}
	private RuleResponse dto(CommissionRule r){return new RuleResponse(r.getId(),r.getScope(),r.getCategory()==null?null:r.getCategory().getId(),r.getCategory()==null?null:r.getCategory().getName(),r.getRatePercent(),r.getEffectiveFrom(),r.getEffectiveTo(),r.isActive(),r.getReason(),r.getUpdatedBy().getFullName(),r.getUpdatedAt());}
}

package com.shopsphere.commission.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.shopsphere.commission.entity.CommissionRule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionRuleRepository extends JpaRepository<CommissionRule,Long>{
	@EntityGraph(attributePaths={"category","updatedBy"}) List<CommissionRule> findAllByOrderByCreatedAtDesc();
	@EntityGraph(attributePaths={"category","updatedBy"}) Optional<CommissionRule> findFirstByScopeAndCategory_IdIsNullAndEffectiveFromLessThanEqualAndActiveTrueOrderByEffectiveFromDesc(CommissionRule.Scope scope,Instant at);
	@EntityGraph(attributePaths={"category","updatedBy"}) Optional<CommissionRule> findFirstByScopeAndCategory_IdAndEffectiveFromLessThanEqualAndActiveTrueOrderByEffectiveFromDesc(CommissionRule.Scope scope,Long categoryId,Instant at);
}

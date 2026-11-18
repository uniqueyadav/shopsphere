package com.shopsphere.user;

import java.util.Optional;

import com.shopsphere.user.entity.User;
import com.shopsphere.common.enums.AccountStatus;
import com.shopsphere.common.enums.Role;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface ShopUserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

	long countByRole(Role role);

	long countByAccountStatus(AccountStatus accountStatus);

	long countByRoleAndAccountStatus(Role role, AccountStatus accountStatus);

	@Override
	Page<User> findAll(Specification<User> specification, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select shopUser from User shopUser where shopUser.id = :userId")
	Optional<User> findByIdForUpdate(@Param("userId") Long userId);
}

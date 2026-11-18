package com.shopsphere.user.repository;

import java.util.List;
import java.util.Optional;

import com.shopsphere.user.entity.Address;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {

	@EntityGraph(attributePaths = "user")
	List<Address> findByUser_IdOrderByDefaultAddressDescCreatedAtDescIdDesc(Long userId);

	@EntityGraph(attributePaths = "user")
	List<Address> findByUser_IdOrderByCreatedAtAscIdAsc(Long userId);

	@EntityGraph(attributePaths = "user")
	List<Address> findAllByUser_Id(Long userId);

	@EntityGraph(attributePaths = "user")
	Optional<Address> findByIdAndUser_Id(Long addressId, Long userId);
}

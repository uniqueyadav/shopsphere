package com.shopsphere.payout.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shopsphere.payout.entity.SellerPayoutAccount;

public interface SellerPayoutAccountRepository extends JpaRepository<SellerPayoutAccount,Long> {
	Optional<SellerPayoutAccount> findBySeller_Id(Long sellerId);
}

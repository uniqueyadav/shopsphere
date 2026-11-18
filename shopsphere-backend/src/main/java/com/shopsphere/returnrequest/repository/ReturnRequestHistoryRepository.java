package com.shopsphere.returnrequest.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shopsphere.returnrequest.entity.ReturnRequestHistory;

public interface ReturnRequestHistoryRepository extends JpaRepository<ReturnRequestHistory, Long> {
	List<ReturnRequestHistory> findByRequest_IdOrderByChangedAtAscIdAsc(Long requestId);
}

package com.shopsphere.refund.repository;

import java.time.Instant;
import java.util.List;

import com.shopsphere.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
	@Query(value = "select o.refund_status as status, count(*) as totalCount, coalesce(sum(p.amount), 0) as totalAmount "
			+ "from payment_transactions p join orders o on o.id = p.order_id join shop_users u on u.id = p.customer_id "
			+ "where o.refund_status <> 'NOT_REQUESTED' and (:search is null or :search = '' "
			+ "or lower(u.full_name) like lower(concat('%', :search, '%')) or lower(u.email) like lower(concat('%', :search, '%')) "
			+ "or cast(o.id as char) like concat('%', :search, '%') or lower(coalesce(p.razorpay_payment_id, '')) like lower(concat('%', :search, '%')) "
			+ "or lower(coalesce(p.razorpay_refund_id, '')) like lower(concat('%', :search, '%'))) "
			+ "and (:status is null or o.refund_status = :status) and (:method is null or o.payment_method = :method) "
			+ "and (:fromDate is null or o.updated_at >= :fromDate) and (:toDate is null or o.updated_at < :toDate) group by o.refund_status",
			nativeQuery = true)
	List<AdminRefundAggregate> summarize(@Param("search") String search, @Param("status") String status,
			@Param("method") String method, @Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate);
}

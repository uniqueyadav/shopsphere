package com.shopsphere.payment.repository;

import java.util.Optional;
import java.util.List;
import java.util.Collection;

import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.payment.entity.Payment;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import com.shopsphere.common.enums.PaymentMethod;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

	@Query(value = "select p.status as status, count(*) as totalCount, coalesce(sum(p.amount), 0) as totalAmount "
			+ "from payment_transactions p join orders o on o.id = p.order_id join shop_users u on u.id = p.customer_id "
			+ "where (:search is null or :search = '' or lower(u.full_name) like lower(concat('%', :search, '%')) "
			+ "or lower(u.email) like lower(concat('%', :search, '%')) or cast(o.id as char) like concat('%', :search, '%') "
			+ "or lower(coalesce(p.razorpay_payment_id, '')) like lower(concat('%', :search, '%')) "
			+ "or lower(p.razorpay_order_id) like lower(concat('%', :search, '%'))) "
			+ "and (:status is null or p.status = :status) and (:method is null or o.payment_method = :method) "
			+ "and (:fromDate is null or p.created_at >= :fromDate) and (:toDate is null or p.created_at < :toDate) group by p.status",
			nativeQuery = true)
	List<AdminPaymentAggregate> summarizeAdminPayments(@Param("search") String search, @Param("status") String status,
			@Param("method") String method, @Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate);

	List<Payment> findByOrder_IdInOrderByCreatedAtDescIdDesc(Collection<Long> orderIds);

	long countByOrder_Id(Long orderId);

	Optional<Payment> findByOrder_IdAndIdempotencyKey(Long orderId, String idempotencyKey);

	Optional<Payment> findFirstByOrder_IdAndStatusOrderByCreatedAtDesc(Long orderId, PaymentStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Payment p where p.order.id = :orderId and p.status = :status order by p.createdAt desc")
	List<Payment> findByOrder_IdAndStatusForUpdate(
			@Param("orderId") Long orderId,
			@Param("status") PaymentStatus status);

	Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Payment p where p.razorpayOrderId = :razorpayOrderId")
	Optional<Payment> findByRazorpayOrderIdForUpdate(@Param("razorpayOrderId") String razorpayOrderId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Payment p where p.razorpayPaymentId = :razorpayPaymentId")
	Optional<Payment> findByRazorpayPaymentIdForUpdate(@Param("razorpayPaymentId") String razorpayPaymentId);

	Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);
}

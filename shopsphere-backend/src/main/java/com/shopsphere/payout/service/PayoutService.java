package com.shopsphere.payout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shopsphere.commission.entity.CommissionRule;
import com.shopsphere.commission.repository.CommissionRuleRepository;
import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.common.enums.RefundStatus;
import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.order.entity.SellerOrder;
import com.shopsphere.payout.dto.PayoutDtos.AdminPayout;
import com.shopsphere.payout.dto.PayoutDtos.PayoutAccount;
import com.shopsphere.payout.dto.PayoutDtos.Summary;
import com.shopsphere.payout.dto.PayoutDtos.Transaction;
import com.shopsphere.payout.dto.PayoutRequest;
import com.shopsphere.payout.dto.PayoutStatusRequest;
import com.shopsphere.payout.entity.PayoutMethod;
import com.shopsphere.payout.entity.PayoutStatus;
import com.shopsphere.payout.entity.SellerPayout;
import com.shopsphere.payout.entity.SellerPayoutAccount;
import com.shopsphere.payout.repository.SellerPayoutAccountRepository;
import com.shopsphere.payout.repository.SellerPayoutRepository;
import com.shopsphere.seller.entity.Seller;
import com.shopsphere.seller.repository.SellerRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class PayoutService {
	private static final BigDecimal ZERO=BigDecimal.ZERO.setScale(2);
	private static final List<PayoutStatus> RESERVED=List.of(PayoutStatus.REQUESTED,PayoutStatus.PROCESSING);
	private final SellerRepository sellers; private final SellerPayoutRepository payouts; private final SellerPayoutAccountRepository accounts;
	private final CommissionRuleRepository commissionRules; private final PayoutEncryptionService encryption;
	private final BigDecimal minimum; private final int cooldownDays;
	@PersistenceContext private EntityManager entityManager;

	public PayoutService(SellerRepository sellers,SellerPayoutRepository payouts,SellerPayoutAccountRepository accounts,
			CommissionRuleRepository commissionRules,PayoutEncryptionService encryption,
			@Value("${shopsphere.payout.minimum-withdrawal:1000.00}") BigDecimal minimum,
			@Value("${shopsphere.payout.cooldown-days:7}") int cooldownDays) {
		this.sellers=sellers;this.payouts=payouts;this.accounts=accounts;this.commissionRules=commissionRules;this.encryption=encryption;this.minimum=minimum;this.cooldownDays=cooldownDays;
	}

	@Transactional(readOnly=true)
	public Summary summary(Long userId,Instant from,Instant to,int page,int size) {
		Seller seller=approvedSeller(userId);
		Instant now=Instant.now(), start=from==null?now.minusSeconds(90L*86400):from, end=to==null?now:to;
		validateRange(start,end,page,size);
		Balance balance=balance(seller.getId(),null);
		BigDecimal pending=payouts.sumBySellerAndStatuses(seller.getId(),RESERVED); if(pending==null)pending=ZERO;
		BigDecimal paid=payouts.sumBySellerAndStatus(seller.getId(),PayoutStatus.PAID); if(paid==null)paid=ZERO;
		Instant monthStart=LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
		BigDecimal month=balance(seller.getId(),monthStart).earnings;
		PayoutAccount account=accounts.findBySeller_Id(seller.getId()).map(a->new PayoutAccount(a.getMethod(),a.getAccountHolder(),a.getMaskedDestination())).orElse(null);
		List<Transaction> all=transactions(seller.getId(),start,end);
		int pages=(int)Math.ceil((double)all.size()/size); int fromIndex=Math.min(page*size,all.size()), toIndex=Math.min(fromIndex+size,all.size());
		return new Summary(balance.earnings.subtract(paid).subtract(pending).max(ZERO),pending,paid,month,account,
				all.subList(fromIndex,toIndex),minimum.intValue(),cooldownDays,all.size(),pages,page,size);
	}

	@Transactional
	public Transaction request(Long userId,String idempotencyKey,PayoutRequest request) {
		Seller seller=sellers.findByUser_IdForUpdate(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Seller profile was not found."));
		if(seller.getApprovalStatus()!=SellerStatus.APPROVED)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only approved sellers can request payouts.");
		if(idempotencyKey==null||idempotencyKey.isBlank()||idempotencyKey.length()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A valid payout request key is required.");
		SellerPayout previous=payouts.findBySeller_IdAndIdempotencyKey(seller.getId(),idempotencyKey).orElse(null);
		if(previous!=null)return map(previous);
		if(request.amount().compareTo(minimum)<0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Minimum payout amount is ₹"+minimum.toPlainString()+".");
		Instant last=payouts.lastOpenOrPaidRequestAt(seller.getId(),PayoutStatus.REJECTED);
		if(last!=null&&last.plusSeconds(cooldownDays*86400L).isAfter(Instant.now()))throw new ResponseStatusException(HttpStatus.CONFLICT,"You can request a payout once every "+cooldownDays+" days.");
		Balance balance=balance(seller.getId(),null);
		BigDecimal pending=payouts.sumBySellerAndStatuses(seller.getId(),RESERVED); if(pending==null)pending=ZERO;
		BigDecimal paid=payouts.sumBySellerAndStatus(seller.getId(),PayoutStatus.PAID); if(paid==null)paid=ZERO;
		BigDecimal available=balance.earnings.subtract(pending).subtract(paid).max(ZERO);
		if(request.amount().compareTo(available)>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Payout amount exceeds your available balance of ₹"+available.toPlainString()+".");
		SellerPayoutAccount account=accounts.findBySeller_Id(seller.getId()).orElse(null);
		String holder,encrypted,masked;
		boolean hasDestination=request.method()==PayoutMethod.UPI?request.upiId()!=null&&!request.upiId().isBlank():request.accountNumber()!=null&&!request.accountNumber().isBlank()&&request.ifsc()!=null&&!request.ifsc().isBlank();
		if(!hasDestination&&account!=null&&account.getMethod()==request.method()) { holder=account.getAccountHolder();encrypted=account.getEncryptedDestination();masked=account.getMaskedDestination(); }
		else {
			validateAccount(request);
			holder=request.accountHolder().trim();encrypted=encryption.encrypt(destination(request));masked=mask(request);
			if(account==null)account=accounts.save(new SellerPayoutAccount(seller,request.method(),holder,encrypted,masked));
			else account.update(request.method(),holder,encrypted,masked);
		}
		if(account==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Register a bank account or UPI ID before requesting payout.");
		SellerPayout payout=payouts.save(new SellerPayout(seller,account,request.amount(),idempotencyKey,request.method(),holder,encrypted,masked));
		return map(payout);
	}

	@Transactional(readOnly=true)
	public org.springframework.data.domain.Page<AdminPayout> adminList(int page,int size) {
		if(page<0||size<1||size>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid page settings.");
		return payouts.findAllByOrderByCreatedAtDesc(PageRequest.of(page,size)).map(p->adminMap(p,true));
	}
	@Transactional
	public AdminPayout updateStatus(Long payoutId,PayoutStatusRequest request) {
		SellerPayout payout=payouts.findByIdForUpdate(payoutId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Payout request was not found."));
		try { switch(request.action().trim().toUpperCase(Locale.ROOT)) {
			case "PROCESS" -> payout.process();
			case "PAID" -> { if(request.transferReference()==null||request.transferReference().isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A bank transfer reference is required before marking a payout paid."); payout.complete(request.transferReference().trim(),request.remarks()); }
			case "REJECT" -> payout.reject(request.remarks()==null?"Rejected by finance.":request.remarks());
			default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported payout action.");
		} } catch(IllegalStateException e){throw new ResponseStatusException(HttpStatus.CONFLICT,e.getMessage());}
		return adminMap(payout,true);
	}

	private Seller approvedSeller(Long userId){Seller s=sellers.findByUser_Id(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Seller profile was not found."));if(s.getApprovalStatus()!=SellerStatus.APPROVED)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only approved sellers can access payouts.");return s;}
	private void validateRange(Instant from,Instant to,int page,int size){if(from==null||to==null||!from.isBefore(to))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Payout date range is invalid.");if(page<0||size<1||size>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid page settings.");}
	private void validateAccount(PayoutRequest r){
		if(r.accountHolder()==null||r.accountHolder().trim().length()<2)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Account holder name is required to register a payout account.");
		if(r.method()==PayoutMethod.BANK_ACCOUNT){if(r.accountNumber()==null||!r.accountNumber().matches("[0-9]{8,34}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bank account number must contain 8 to 34 digits.");if(r.ifsc()==null||!r.ifsc().trim().toUpperCase(Locale.ROOT).matches("[A-Z]{4}0[A-Z0-9]{6}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a valid IFSC code.");}
		if(r.method()==PayoutMethod.UPI&&(r.upiId()==null||!r.upiId().trim().matches("[A-Za-z0-9._-]{2,256}@[A-Za-z]{2,64}")))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a valid UPI ID.");
	}
	private String destination(PayoutRequest r){return r.method()==PayoutMethod.UPI?r.upiId().trim():r.accountNumber().trim()+"|"+r.ifsc().trim().toUpperCase(Locale.ROOT);}
	private String mask(PayoutRequest r){return r.method()==PayoutMethod.UPI?maskText(r.upiId()):"••••"+r.accountNumber().substring(Math.max(0,r.accountNumber().length()-4))+" · "+r.ifsc().toUpperCase(Locale.ROOT);}
	private String maskText(String value){int at=value.lastIndexOf('@');return value.substring(0,Math.min(2,at))+"••••"+value.substring(at);}
	private Balance balance(Long sellerId,Instant from){
		Instant start=from==null?Instant.EPOCH:from; Instant now=Instant.now();
		List<Object[]> rows=entityManager.createQuery("select so.subtotal, so.order.createdAt, so.order.refundStatus, item.lineSubtotal, category.id " +
				"from SellerOrder so join so.items item join item.product product join product.category category where so.seller.id=:seller " +
				"and so.order.paymentStatus in :paid and so.orderStatus=:delivered and so.order.createdAt>=:start and so.order.createdAt<:end",Object[].class)
				.setParameter("seller",sellerId).setParameter("paid",List.of(PaymentStatus.PAID,PaymentStatus.SUCCESS)).setParameter("delivered",OrderStatus.DELIVERED).setParameter("start",start).setParameter("end",now).getResultList();
		List<CommissionRule> rules=commissionRules.findAllByOrderByCreatedAtDesc().stream().filter(CommissionRule::isActive).toList();
		BigDecimal earned=ZERO;
		for(Object[] row:rows){if(row[2]==RefundStatus.REFUNDED)continue;BigDecimal line=(BigDecimal)row[3];BigDecimal rate=rate(rules,(Long)row[4],(Instant)row[1]);if(rate==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"A commission rule is missing for eligible earnings; contact support before requesting payout.");earned=earned.add(line.subtract(line.multiply(rate).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP)));}
		return new Balance(earned);
	}
	private BigDecimal rate(List<CommissionRule> rules,Long categoryId,Instant at){var cat=rules.stream().filter(r->r.getScope()==CommissionRule.Scope.CATEGORY&&r.getCategory()!=null&&categoryId.equals(r.getCategory().getId())&&effective(r,at)).max(Comparator.comparing(CommissionRule::getEffectiveFrom));if(cat.isPresent())return cat.get().getRatePercent();return rules.stream().filter(r->r.getScope()==CommissionRule.Scope.PLATFORM&&effective(r,at)).max(Comparator.comparing(CommissionRule::getEffectiveFrom)).map(CommissionRule::getRatePercent).orElse(null);}
	private boolean effective(CommissionRule r,Instant at){return !r.getEffectiveFrom().isAfter(at)&&(r.getEffectiveTo()==null||r.getEffectiveTo().isAfter(at));}
	private List<Transaction> transactions(Long sellerId,Instant from,Instant to){
		List<Transaction> out=new ArrayList<>();
		List<Object[]> rows=entityManager.createQuery("select so.id, so.order.id, so.order.createdAt, item.lineSubtotal, so.order.refundStatus, so.orderStatus, item.product.category.id, item.product.productName from SellerOrder so join so.items item where so.seller.id=:seller and so.order.paymentStatus in :paid and so.order.createdAt>=:from and so.order.createdAt<:to order by so.order.createdAt desc",Object[].class)
				.setParameter("seller",sellerId).setParameter("paid",List.of(PaymentStatus.PAID,PaymentStatus.SUCCESS)).setParameter("from",from).setParameter("to",to).getResultList();
		Map<Long,TransactionBuilder> sales=new TreeMap<>();
		List<CommissionRule> rules=commissionRules.findAllByOrderByCreatedAtDesc().stream().filter(CommissionRule::isActive).toList();
		for(Object[] row:rows){Long sellerOrderId=(Long)row[0];BigDecimal line=(BigDecimal)row[3];boolean refund=row[4]==RefundStatus.REFUNDED;TransactionBuilder builder=sales.computeIfAbsent(sellerOrderId,k->new TransactionBuilder(sellerOrderId,(Long)row[1],(Instant)row[2],row[5].toString()));BigDecimal rate=rate(rules,(Long)row[6],(Instant)row[2]);BigDecimal net=rate==null?line:line.subtract(line.multiply(rate).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP));builder.amount=builder.amount.add(refund?line.negate():net);builder.refund|=refund;builder.name=(String)row[7];}
		for(TransactionBuilder tx:sales.values())out.add(new Transaction(tx.id,tx.date,tx.refund?"REFUND":"ORDER_EARNING",tx.amount,tx.refund?"REFUNDED":"CLEARED","ORDER-"+tx.orderId,tx.refund?"Order refund":"Delivered order · "+tx.name,tx.orderId));
		payouts.findBySeller_IdAndCreatedAtBetween(sellerId,from,to,PageRequest.of(0,1000,Sort.by(Sort.Direction.DESC,"createdAt"))).getContent().forEach(p->out.add(map(p)));
		out.sort(Comparator.comparing(Transaction::date).reversed());return out;
	}
	private Transaction map(SellerPayout p){return new Transaction(p.getId(),p.getCreatedAt(),"PAYOUT",p.getAmount().negate(),p.getStatus().name(),"PAYOUT-"+p.getId(),p.getRemarks()==null?p.getMaskedDestination():p.getRemarks(),null);}
	private AdminPayout adminMap(SellerPayout p,boolean includeDestination){String dest=includeDestination?encryption.decrypt(p.getEncryptedDestination()):null;return new AdminPayout(p.getId(),p.getSeller().getId(),p.getSeller().getStoreName(),p.getAmount(),p.getStatus(),p.getMethod(),p.getAccountHolder(),p.getMaskedDestination(),dest,p.getTransferReference(),p.getRemarks(),p.getCreatedAt());}
	private record Balance(BigDecimal earnings){}
	private static final class TransactionBuilder{final Long id,orderId;final Instant date;final String status;BigDecimal amount=ZERO;boolean refund;String name;TransactionBuilder(Long i,Long o,Instant d,String s){id=i;orderId=o;date=d;status=s;}}
}

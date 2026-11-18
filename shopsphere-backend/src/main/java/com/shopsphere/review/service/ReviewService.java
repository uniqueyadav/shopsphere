package com.shopsphere.review.service;

import java.util.Locale;
import java.util.Map;

import com.shopsphere.common.enums.ReviewStatus;
import com.shopsphere.review.dto.ReviewRequest;
import com.shopsphere.review.dto.ReviewResponse;
import com.shopsphere.review.entity.Review;
import com.shopsphere.review.repository.ReviewRepository;
import com.shopsphere.user.ShopUserRepository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class ReviewService {
	private final ReviewRepository reviews;
	private final ShopUserRepository users;
	public ReviewService(ReviewRepository reviews, ShopUserRepository users) { this.reviews=reviews; this.users=users; }
	@Transactional(readOnly=true)
	public Page<ReviewResponse> list(String search, Integer rating, ReviewStatus status, Long productId, int page, int size) {
		if(page<0 || size<1 || size>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pagination values.");
		Specification<Review> spec=(root,q,cb)-> { var predicates=new java.util.ArrayList<Predicate>();
			if(status!=null) predicates.add(cb.equal(root.get("status"),status));
			if(rating!=null) predicates.add(cb.equal(root.get("rating"),rating));
			if(productId!=null) predicates.add(cb.equal(root.get("product").get("id"),productId));
			if(search!=null && !search.isBlank()) { String term="%"+search.trim().toLowerCase(Locale.ROOT)+"%"; predicates.add(cb.or(cb.like(cb.lower(root.get("product").get("productName")),term),cb.like(cb.lower(root.get("product").get("sku")),term),cb.like(cb.lower(root.get("customer").get("fullName")),term),cb.like(cb.lower(root.get("customer").get("email")),term),cb.like(cb.lower(root.get("reviewText")),term))); }
			return cb.and(predicates.toArray(Predicate[]::new)); };
		return reviews.findAll(spec,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt"))).map(this::response);
	}
	@Transactional(readOnly=true)
	public ReviewResponse get(Long id) { return response(reviews.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Review not found."))); }
	@Transactional(readOnly=true)
	public Map<String,Long> stats() { long total=reviews.count(), positive=reviews.countByRatingGreaterThanEqual(4), neutral=reviews.countByRating(3), negative=reviews.countByRating(2)+reviews.countByRating(1); return Map.of("totalReviews",total,"positiveReviews",positive,"neutralReviews",neutral,"negativeReviews",negative); }
	@Transactional
	public ReviewResponse moderate(Long id, ReviewRequest request, Long adminId) { Review review=reviews.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Review not found.")); if(request.status()==ReviewStatus.PENDING) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A review cannot be moderated back to pending."); if(request.status()==ReviewStatus.REJECTED && (request.rejectionReason()==null || request.rejectionReason().isBlank())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A rejection reason is required."); var admin=users.findById(adminId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Administrator not found.")); review.moderate(request.status(),request.adminNote(),request.rejectionReason(),admin); return response(review); }
	@Transactional
	public void delete(Long id) { if(!reviews.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Review not found."); reviews.deleteById(id); }
	private ReviewResponse response(Review r) { var p=r.getProduct(); var c=r.getCustomer(); return new ReviewResponse(r.getId(),p.getId(),p.getProductName(),p.getSku(),null,p.getCategory().getName(),p.getSeller().getStoreName(),c.getId(),c.getFullName(),c.getEmail(),r.getRating(),r.getReviewText(),r.getStatus(),r.getAdminNote(),r.getRejectionReason(),r.getCreatedAt(),r.getModeratedAt(),reviews.countByProduct_IdAndStatus(p.getId(),ReviewStatus.APPROVED),reviews.averageForProduct(p.getId())==null?0:reviews.averageForProduct(p.getId())); }
}

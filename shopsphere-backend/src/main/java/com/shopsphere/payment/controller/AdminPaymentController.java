package com.shopsphere.payment.controller;

import java.time.LocalDate;
import java.util.List;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.payment.dto.AdminPaymentPageResponse;
import com.shopsphere.payment.dto.AdminPaymentResponse;
import com.shopsphere.payment.dto.AdminPaymentStatsResponse;
import com.shopsphere.payment.service.AdminPaymentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/payments")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminPaymentController {
	private final AdminPaymentService service;
	public AdminPaymentController(AdminPaymentService service) { this.service = service; }

	@GetMapping
	public ApiResponse<AdminPaymentPageResponse> list(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size, @RequestParam(required=false) String search, @RequestParam(required=false) String status, @RequestParam(required=false) String method, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
		return ApiResponse.success("Payments loaded.", service.list(page, size, search, status, method, from, to));
	}
	@GetMapping("/stats")
	public ApiResponse<AdminPaymentStatsResponse> stats(@RequestParam(required=false) String search, @RequestParam(required=false) String status, @RequestParam(required=false) String method, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
		return ApiResponse.success("Payment statistics loaded.", service.stats(search, status, method, from, to));
	}
	@GetMapping("/{paymentId}")
	public ApiResponse<AdminPaymentResponse> get(@PathVariable Long paymentId) { return ApiResponse.success("Payment details loaded.", service.get(paymentId)); }
	@GetMapping(value="/export", produces="text/csv")
	public ResponseEntity<String> export(@RequestParam(required=false) String search, @RequestParam(required=false) String status, @RequestParam(required=false) String method, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
		List<AdminPaymentResponse> rows = service.export(search, status, method, from, to);
		StringBuilder csv = new StringBuilder("Payment ID,Order ID,Customer,Email,Method,Amount,Currency,Status,Transaction ID,Gateway Order ID,Created At\n");
		for (var row : rows) csv.append(csv(row.id())).append(',').append(csv(row.orderId())).append(',').append(csv(row.customerName())).append(',').append(csv(row.customerEmail())).append(',').append(csv(row.paymentMethod())).append(',').append(csv(row.amount())).append(',').append(csv(row.currency())).append(',').append(csv(row.status())).append(',').append(csv(row.transactionId())).append(',').append(csv(row.gatewayOrderId())).append(',').append(csv(row.createdAt())).append('\n');
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shopsphere-payments.csv").contentType(MediaType.parseMediaType("text/csv; charset=UTF-8")).body(csv.toString());
	}
	private String csv(Object value) { String text = value == null ? "" : value.toString(); return "\"" + text.replace("\"", "\"\"") + "\""; }
}

package com.shopsphere.refund.controller;

import java.time.LocalDate;
import java.util.List;

import com.shopsphere.common.response.ApiResponse;
import com.shopsphere.refund.dto.AdminRefundPageResponse;
import com.shopsphere.refund.dto.AdminRefundResponse;
import com.shopsphere.refund.dto.AdminRefundStatsResponse;
import com.shopsphere.refund.service.RefundService;
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
@RequestMapping("/api/v1/admin/refunds")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class RefundController {
	private final RefundService refunds;
	public RefundController(RefundService refunds) { this.refunds = refunds; }

	@GetMapping
	public ApiResponse<AdminRefundPageResponse> list(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size, @RequestParam(required=false) String search, @RequestParam(required=false) String status, @RequestParam(required=false) String method, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
		return ApiResponse.success("Refund records loaded.", refunds.list(page, size, search, status, method, from, to));
	}
	@GetMapping("/stats")
	public ApiResponse<AdminRefundStatsResponse> stats(@RequestParam(required=false) String search, @RequestParam(required=false) String status, @RequestParam(required=false) String method, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
		return ApiResponse.success("Refund statistics loaded.", refunds.stats(search, status, method, from, to));
	}
	@GetMapping("/{paymentTransactionId}")
	public ApiResponse<AdminRefundResponse> get(@PathVariable Long paymentTransactionId) { return ApiResponse.success("Refund details loaded.", refunds.get(paymentTransactionId)); }
	@GetMapping(value="/export", produces="text/csv")
	public ResponseEntity<String> export(@RequestParam(required=false) String search, @RequestParam(required=false) String status, @RequestParam(required=false) String method, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
		List<AdminRefundResponse> rows = refunds.export(search, status, method, from, to);
		StringBuilder csv = new StringBuilder("Payment Transaction,Order ID,Gateway Refund ID,Customer,Email,Method,Order Amount,Refund Amount,Currency,Status,Order Date\n");
		for (var row : rows) csv.append(csv(row.paymentTransactionId())).append(',').append(csv(row.orderId())).append(',').append(csv(row.refundReference())).append(',').append(csv(row.customerName())).append(',').append(csv(row.customerEmail())).append(',').append(csv(row.paymentMethod())).append(',').append(csv(row.orderAmount())).append(',').append(csv(row.refundAmount())).append(',').append(csv(row.currency())).append(',').append(csv(row.status())).append(',').append(csv(row.orderDate())).append('\n');
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shopsphere-refunds.csv").contentType(MediaType.parseMediaType("text/csv; charset=UTF-8")).body(csv.toString());
	}
	private String csv(Object value) { String text = value == null ? "" : value.toString(); if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) text = "'" + text; return "\"" + text.replace("\"", "\"\"") + "\""; }
}

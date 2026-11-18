package com.shopsphere.analytics.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.shopsphere.analytics.dto.RevenueReportResponse;

import com.shopsphere.analytics.dto.DashboardResponse;
import com.shopsphere.analytics.service.AdminAnalyticsService;
import com.shopsphere.common.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/admin/analytics")
public class AnalyticsController {
	private final AdminAnalyticsService analyticsService;

	public AnalyticsController(AdminAnalyticsService analyticsService) { this.analyticsService = analyticsService; }

	@GetMapping("/dashboard")
	public ApiResponse<DashboardResponse> getDashboard(@RequestParam(defaultValue = "SIX_MONTHS") String period) {
		return ApiResponse.success("Dashboard analytics loaded.", analyticsService.getDashboard(period));
	}

	@GetMapping("/revenue")
	public ApiResponse<?> getRevenue(@RequestParam(required=false) Instant from, @RequestParam(required=false) Instant to) {
		return ApiResponse.success("Revenue report loaded.", analyticsService.getRevenueReport(from, to));
	}

	@GetMapping(value="/revenue/export", produces="text/csv")
	public ResponseEntity<String> exportRevenue(@RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to){
		RevenueReportResponse report=analyticsService.getRevenueReport(from,to);
		StringBuilder csv=new StringBuilder("ShopSphere Revenue Report\nPeriod Start,Period End\n").append(report.from()).append(',').append(report.to()).append("\n\nMetric,Value\nPaid Order Total,").append(report.paidOrderTotal()).append("\nPaid Orders,").append(report.paidOrderCount()).append("\n\nOrder ID,Customer,Amount,Payment Status,Order Status,Created At\n");
		for(var row:report.recentTransactions())csv.append(row.orderId()).append(',').append(csv(row.customerName())).append(',').append(row.amount()).append(',').append(row.paymentStatus()).append(',').append(row.orderStatus()).append(',').append(row.createdAt()).append('\n');
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=shopsphere-revenue-report.csv").contentType(MediaType.parseMediaType("text/csv; charset=UTF-8")).body(csv.toString());
	}
	private String csv(String value){String text=value==null?"":value;if(!text.isEmpty()&&"=+-@".indexOf(text.charAt(0))>=0)text="'"+text;return "\""+text.replace("\"","\"\"")+"\"";}
}

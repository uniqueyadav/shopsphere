package com.shopsphere.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopsphere.analytics.dto.DashboardResponse;
import com.shopsphere.analytics.dto.DashboardResponse.Metric;
import com.shopsphere.analytics.dto.DashboardResponse.RecentOrder;
import com.shopsphere.analytics.dto.DashboardResponse.StatusCount;
import com.shopsphere.analytics.dto.DashboardResponse.TopProduct;
import com.shopsphere.analytics.dto.DashboardResponse.TrendPoint;
import com.shopsphere.analytics.dto.RevenueReportResponse;
import com.shopsphere.analytics.repository.AnalyticsRepository;
import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.common.enums.Role;
import com.shopsphere.common.enums.SellerStatus;
import com.shopsphere.order.entity.Order;

@Service
@Transactional(readOnly = true)
public class AdminAnalyticsService {
	private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("d MMM");
	private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yy");
	private final AnalyticsRepository analytics;

	public AdminAnalyticsService(AnalyticsRepository analytics) { this.analytics = analytics; }

	public DashboardResponse getDashboard(String requestedPeriod) {
		String period = normalizePeriod(requestedPeriod);
		Instant now = Instant.now();
		Instant start = startFor(period, now);
		Instant previousStart = period.equals("ALL_TIME") ? start : start.minus(periodDays(period), ChronoUnit.DAYS);
		Instant previousEnd = start;
		BigDecimal revenue = analytics.sumRevenue(start, now);
		BigDecimal previousRevenue = period.equals("ALL_TIME") ? BigDecimal.ZERO : analytics.sumRevenue(previousStart, previousEnd);
		long orderCount = analytics.countOrders(start, now);
		long previousOrderCount = period.equals("ALL_TIME") ? 0 : analytics.countOrders(previousStart, previousEnd);
		long customerCount = analytics.countUsers(Role.CUSTOMER);
		long newCustomers = analytics.countUsers(Role.CUSTOMER, start, now);
		long previousCustomers = period.equals("ALL_TIME") ? 0 : analytics.countUsers(Role.CUSTOMER, previousStart, previousEnd);
		List<BigDecimal> customerSparkline = makeCountSparkline(analytics.userCreatedAt(Role.CUSTOMER, start, now), period, now);
		long sellerCount = analytics.countSellers(SellerStatus.APPROVED);
		long newSellers = analytics.countSellers(SellerStatus.APPROVED, start, now);
		long previousSellers = period.equals("ALL_TIME") ? 0 : analytics.countSellers(SellerStatus.APPROVED, previousStart, previousEnd);
		List<BigDecimal> sellerSparkline = makeCountSparkline(analytics.sellerCreatedAt(SellerStatus.APPROVED, start, now), period, now);
		List<TrendPoint> trend = makeTrend(analytics.orderFacts(start, now), period, now);
		List<StatusCount> statuses = analytics.statusCounts(start, now).stream()
				.map(row -> new StatusCount(((OrderStatus) row[0]).name(), ((Number) row[1]).longValue())).toList();
		List<RecentOrder> recent = analytics.recentOrders(start, now, 6).stream().map(this::toRecentOrder).toList();
		List<Object[]> products = analytics.topProducts(start, now, 5);
		Map<Long, String> images = new HashMap<>();
		analytics.productImages(products.stream().map(row -> (Long) row[0]).toList())
				.forEach(row -> images.put((Long) row[0], (String) row[1]));
		List<TopProduct> topProducts = products.stream().map(row -> new TopProduct(
				(Long) row[0], (String) row[1], images.get((Long) row[0]),
				((Number) row[2]).longValue(), (BigDecimal) row[3])).toList();
		return new DashboardResponse(period, now,
				metric(revenue, previousRevenue, trend.stream().map(TrendPoint::revenue).toList()),
				metric(BigDecimal.valueOf(orderCount), BigDecimal.valueOf(previousOrderCount), trend.stream().map(TrendPoint::orders).map(BigDecimal::valueOf).toList()),
				metricComparedWithPeriod(BigDecimal.valueOf(customerCount), BigDecimal.valueOf(newCustomers), BigDecimal.valueOf(previousCustomers), customerSparkline),
				metricComparedWithPeriod(BigDecimal.valueOf(sellerCount), BigDecimal.valueOf(newSellers), BigDecimal.valueOf(previousSellers), sellerSparkline),
				trend, orderCount, statuses, recent, topProducts);
	}

	public RevenueReportResponse getRevenueReport(Instant from, Instant to) {
		Instant end = to == null ? Instant.now() : to;
		Instant start = from == null ? end.minus(30, ChronoUnit.DAYS) : from;
		if (!start.isBefore(end)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Start date must be before end date.");
		List<RevenueReportResponse.RevenuePoint> points = analytics.paidRevenueByDay(start, end).stream()
				.map(row -> new RevenueReportResponse.RevenuePoint(String.valueOf(row[0]), (BigDecimal) row[1], ((Number) row[2]).longValue())).toList();
		List<RevenueReportResponse.RecentTransaction> transactions = analytics.recentPaidOrderFacts(start, end, 20).stream()
				.map(row -> new RevenueReportResponse.RecentTransaction(((Number) row[0]).longValue(), (String) row[1], (BigDecimal) row[2], ((PaymentStatus) row[3]).name(), ((OrderStatus) row[4]).name(), (Instant) row[5])).toList();
		BigDecimal total=analytics.sumRevenue(start,end);
		List<Object[]> categoryRows=analytics.paidCategoryRevenue(start,end,5);
		BigDecimal productSales=categoryRows.stream().map(row->(BigDecimal)row[3]).reduce(BigDecimal.ZERO,BigDecimal::add);
		List<RevenueReportResponse.CategoryRevenue> categories=categoryRows.stream().map(row->{BigDecimal sales=(BigDecimal)row[3];BigDecimal share=productSales.signum()==0?BigDecimal.ZERO:sales.multiply(BigDecimal.valueOf(100)).divide(productSales,2,RoundingMode.HALF_UP);return new RevenueReportResponse.CategoryRevenue(((Number)row[0]).longValue(),(String)row[1],(String)row[2],sales,share);}).toList();
		return new RevenueReportResponse(start, end, total, analytics.countPaidOrders(start, end), points, transactions,categories);
	}

	private RecentOrder toRecentOrder(Order order) {
		return new RecentOrder(order.getId(), order.getCustomer().getFullName(), order.getCreatedAt(), order.getTotalAmount(), order.getOrderStatus().name());
	}

	private Metric metric(BigDecimal value, BigDecimal previous, List<BigDecimal> sparkline) {
		BigDecimal growth = previous.signum() == 0 ? null
				: value.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous.abs(), 1, RoundingMode.HALF_UP);
		return new Metric(value, growth, sparkline);
	}

	private Metric metricComparedWithPeriod(BigDecimal total, BigDecimal currentPeriod, BigDecimal previousPeriod, List<BigDecimal> sparkline) {
		BigDecimal growth = previousPeriod.signum() == 0 ? null
				: currentPeriod.subtract(previousPeriod).multiply(BigDecimal.valueOf(100)).divide(previousPeriod.abs(), 1, RoundingMode.HALF_UP);
		return new Metric(total, growth, sparkline);
	}

	private List<TrendPoint> makeTrend(List<Object[]> facts, String period, Instant now) {
		boolean daily = period.equals("THIRTY_DAYS");
		boolean yearly = period.equals("ALL_TIME");
		Map<String, BigDecimal> revenue = new LinkedHashMap<>();
		Map<String, Long> orders = new LinkedHashMap<>();
		for (Object[] fact : facts) {
			Instant created = (Instant) fact[0];
			LocalDate createdDate = LocalDate.ofInstant(created, ZoneOffset.UTC);
			String key = daily ? createdDate.toString() : yearly ? Integer.toString(createdDate.getYear())
					: YearMonth.from(createdDate).toString();
			orders.merge(key, 1L, Long::sum);
			if (fact[2] == PaymentStatus.SUCCESS) revenue.merge(key, (BigDecimal) fact[1], BigDecimal::add);
		}
		if (yearly) {
			int currentYear = Year.now(ZoneOffset.UTC).getValue();
			int firstYear = revenue.keySet().stream().mapToInt(Integer::parseInt).min().orElse(currentYear);
			for (int year = firstYear; year <= currentYear; year++) { revenue.putIfAbsent(Integer.toString(year), BigDecimal.ZERO); orders.putIfAbsent(Integer.toString(year), 0L); }
		} else {
			int bucketCount = daily ? 30 : period.equals("ONE_YEAR") ? 12 : 6;
			for (int i = bucketCount - 1; i >= 0; i--) {
				String key = daily ? LocalDate.ofInstant(now, ZoneOffset.UTC).minusDays(i).toString()
						: YearMonth.from(LocalDate.ofInstant(now, ZoneOffset.UTC)).minusMonths(i).toString();
				revenue.putIfAbsent(key, BigDecimal.ZERO);
				orders.putIfAbsent(key, 0L);
			}
		}
		List<String> keys = revenue.keySet().stream().sorted().toList();
		int stride = yearly ? 1 : Math.max(1, (int) Math.ceil(keys.size() / 8.0));
		List<TrendPoint> result = new ArrayList<>();
		for (int i = 0; i < keys.size(); i += stride) result.add(point(keys.get(i), daily, yearly, revenue, orders));
		if (!keys.isEmpty() && (keys.size() - 1) % stride != 0) result.add(point(keys.get(keys.size() - 1), daily, yearly, revenue, orders));
		return result;
	}

	private TrendPoint point(String key, boolean daily, boolean yearly, Map<String, BigDecimal> revenue, Map<String, Long> orders) {
		String label = daily ? LocalDate.parse(key).format(DAY_LABEL) : yearly ? key : YearMonth.parse(key).format(MONTH_LABEL);
		return new TrendPoint(label, revenue.get(key), orders.get(key));
	}

	private List<BigDecimal> makeCountSparkline(List<Instant> createdAt, String period, Instant now) {
		boolean daily = period.equals("THIRTY_DAYS");
		boolean yearly = period.equals("ALL_TIME");
		Map<String, Long> counts = new java.util.TreeMap<>();
		for (Instant created : createdAt) {
			LocalDate date = LocalDate.ofInstant(created, ZoneOffset.UTC);
			String key = daily ? date.toString() : yearly ? Integer.toString(date.getYear()) : YearMonth.from(date).toString();
			counts.merge(key, 1L, Long::sum);
		}
		if (yearly) {
			int currentYear = Year.now(ZoneOffset.UTC).getValue();
			int firstYear = counts.keySet().stream().mapToInt(Integer::parseInt).min().orElse(currentYear);
			for (int year = firstYear; year <= currentYear; year++) counts.putIfAbsent(Integer.toString(year), 0L);
		} else {
			int bucketCount = daily ? 30 : period.equals("ONE_YEAR") ? 12 : 6;
			for (int i = bucketCount - 1; i >= 0; i--) {
				String key = daily ? LocalDate.ofInstant(now, ZoneOffset.UTC).minusDays(i).toString()
						: YearMonth.from(LocalDate.ofInstant(now, ZoneOffset.UTC)).minusMonths(i).toString();
				counts.putIfAbsent(key, 0L);
			}
		}
		return counts.values().stream().map(BigDecimal::valueOf).toList();
	}

	private String normalizePeriod(String period) {
		if (period == null) return "SIX_MONTHS";
		return switch (period.toUpperCase()) {
			case "THIRTY_DAYS", "ONE_YEAR", "ALL_TIME" -> period.toUpperCase();
			default -> "SIX_MONTHS";
		};
	}

	private Instant startFor(String period, Instant now) {
		return switch (period) {
			case "THIRTY_DAYS" -> now.minus(30, ChronoUnit.DAYS);
			case "ONE_YEAR" -> now.minus(365, ChronoUnit.DAYS);
			case "ALL_TIME" -> Instant.parse("1970-01-01T00:00:00Z");
			default -> now.minus(180, ChronoUnit.DAYS);
		};
	}

	private long periodDays(String period) {
		return switch (period) { case "THIRTY_DAYS" -> 30; case "ONE_YEAR" -> 365; default -> 180; };
	}
}

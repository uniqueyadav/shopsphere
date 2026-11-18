package com.shopsphere.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.shopsphere.analytics.dto.DashboardResponse;
import com.shopsphere.analytics.service.AdminAnalyticsService;

@SpringBootTest
@ActiveProfiles("test")
class AdminAnalyticsServiceTests {
	@Autowired private AdminAnalyticsService dashboardService;

	@Test
	void returnsDatabaseBackedEmptyDashboardForAvailablePeriods() {
		DashboardResponse recent = dashboardService.getDashboard("THIRTY_DAYS");
		DashboardResponse allTime = dashboardService.getDashboard("ALL_TIME");

		assertThat(recent.period()).isEqualTo("THIRTY_DAYS");
		assertThat(recent.totalOrders()).isZero();
		assertThat(recent.revenue().value()).isZero();
		assertThat(recent.trend()).isNotEmpty();
		assertThat(recent.recentOrders()).isEmpty();
		assertThat(recent.topProducts()).isEmpty();
		assertThat(allTime.period()).isEqualTo("ALL_TIME");
		assertThat(allTime.trend()).isNotEmpty();
	}
}

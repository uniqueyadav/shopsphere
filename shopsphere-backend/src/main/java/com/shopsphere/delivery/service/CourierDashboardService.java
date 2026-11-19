package com.shopsphere.delivery.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.delivery.dto.CourierDashboardResponse;
import com.shopsphere.delivery.dto.CourierDashboardResponse.RecentDelivery;
import com.shopsphere.delivery.dto.CourierDashboardResponse.TrendDay;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourierDashboardService {
    private static final ZoneId MARKETPLACE_ZONE = ZoneId.of("Asia/Kolkata");
    private final EntityManager entityManager;

    public CourierDashboardService(EntityManager entityManager) { this.entityManager = entityManager; }

    @Transactional(readOnly = true)
    public CourierDashboardResponse dashboard(int days) {
        LocalDate today = LocalDate.now(MARKETPLACE_ZONE);
        LocalDate from = today.minusDays(days - 1L);
        var start = from.atStartOfDay(MARKETPLACE_ZONE).toInstant();
        var end = today.plusDays(1).atStartOfDay(MARKETPLACE_ZONE).toInstant();
        long total = count(start, end, null);
        long delivered = count(start, end, OrderStatus.DELIVERED);
        long transit = count(start, end, OrderStatus.SHIPPED) + count(start, end, OrderStatus.OUT_FOR_DELIVERY);
        long failed = count(start, end, OrderStatus.CANCELLED);

        List<Object[]> grouped = entityManager.createQuery(
                "select function('date', so.order.createdAt), so.orderStatus, count(so) " +
                "from SellerOrder so where so.order.createdAt >= :start and so.order.createdAt < :end " +
                "group by function('date', so.order.createdAt), so.orderStatus", Object[].class)
                .setParameter("start", start).setParameter("end", end).getResultList();
        Map<LocalDate, long[]> byDate = new HashMap<>();
        for (Object[] row : grouped) {
            LocalDate date = row[0] instanceof java.sql.Date d ? d.toLocalDate() : LocalDate.parse(row[0].toString());
            long[] counts = byDate.computeIfAbsent(date, ignored -> new long[3]);
            long n = ((Number) row[2]).longValue();
            counts[0] += n;
            if (row[1] == OrderStatus.DELIVERED) counts[1] += n;
            if (row[1] == OrderStatus.CANCELLED) counts[2] += n;
        }
        List<TrendDay> trend = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = from.plusDays(i);
            long[] counts = byDate.getOrDefault(date, new long[3]);
            trend.add(new TrendDay(date, counts[0], counts[1], counts[2]));
        }
        List<RecentDelivery> recent = entityManager.createQuery(
                "select so.id, so.order.id, so.order.shippingFullName, so.sellerName, so.orderStatus, " +
                "so.order.shippingCity, so.order.createdAt from SellerOrder so " +
                "where so.order.createdAt >= :start and so.order.createdAt < :end " +
                "order by so.order.createdAt desc, so.id desc", Object[].class)
                .setParameter("start", start).setParameter("end", end).setMaxResults(8).getResultList()
                .stream().map(r -> new RecentDelivery((Long) r[0], (Long) r[1], (String) r[2],
                        (String) r[3], ((OrderStatus) r[4]).name(), (String) r[5], (java.time.Instant) r[6]))
                .toList();
        return new CourierDashboardResponse(total, delivered, transit, failed, trend, recent);
    }

    private long count(java.time.Instant start, java.time.Instant end, OrderStatus status) {
        String query = "select count(so) from SellerOrder so where so.order.createdAt >= :start and so.order.createdAt < :end";
        var typed = entityManager.createQuery(query + (status == null ? "" : " and so.orderStatus = :status"), Long.class)
                .setParameter("start", start).setParameter("end", end);
        if (status != null) typed.setParameter("status", status);
        return typed.getSingleResult();
    }
}

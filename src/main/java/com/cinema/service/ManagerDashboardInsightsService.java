package com.cinema.service;

import com.cinema.dao.RevenueReportDao;
import com.cinema.model.DashboardRevenueDay;
import com.cinema.model.report.DailyRevenueItem;
import com.cinema.model.report.TransactionRevenueItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Revenue widgets of the Manager Dashboard (M01): the 7-day revenue chart and the latest bookings table.
 * Reuses the queries of the Revenue Report module so both screens always show the same figures.
 */
@Service
public class ManagerDashboardInsightsService {

    static final int CHART_DAYS = 7;

    private final RevenueReportDao revenueReportDao;

    public ManagerDashboardInsightsService(RevenueReportDao revenueReportDao) {
        this.revenueReportDao = revenueReportDao;
    }

    /** Ticket and F&B revenue for each of the last 7 days (oldest first, today last); missing days are zero. */
    public List<DashboardRevenueDay> getWeeklyRevenue() {
        return buildWeeklyRevenue(LocalDate.now());
    }

    List<DashboardRevenueDay> buildWeeklyRevenue(LocalDate today) {
        LocalDate start = today.minusDays(CHART_DAYS - 1L);
        List<DailyRevenueItem> rows = safe(revenueReportDao.getDailyRevenue(start, today));
        Map<LocalDate, DailyRevenueItem> byDate = rows.stream()
                .filter(r -> r.getDate() != null)
                .collect(Collectors.toMap(DailyRevenueItem::getDate, Function.identity(), (a, b) -> a));

        List<DashboardRevenueDay> days = new ArrayList<>();
        BigDecimal max = BigDecimal.ZERO;
        for (int i = 0; i < CHART_DAYS; i++) {
            LocalDate date = start.plusDays(i);
            DailyRevenueItem row = byDate.get(date);
            DashboardRevenueDay day = new DashboardRevenueDay();
            day.setDate(date);
            day.setToday(date.equals(today));
            day.setLabel(day.isToday() ? "Today" : date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            day.setTicketRevenue(nz(row == null ? null : row.getTicketRevenue()));
            day.setFbRevenue(nz(row == null ? null : row.getFbRevenue()));
            max = max.max(day.getTicketRevenue()).max(day.getFbRevenue());
            days.add(day);
        }
        for (DashboardRevenueDay day : days) {
            day.setTicketHeightPercent(percent(day.getTicketRevenue(), max));
            day.setFbHeightPercent(percent(day.getFbRevenue(), max));
        }
        return days;
    }

    /** The most recent paid/confirmed bookings of the last 7 days. */
    public List<TransactionRevenueItem> getRecentBookings(int limit) {
        LocalDate today = LocalDate.now();
        return safe(revenueReportDao.getTransactions(today.minusDays(CHART_DAYS - 1L), today))
                .stream().limit(limit).collect(Collectors.toList());
    }

    private static int percent(BigDecimal value, BigDecimal max) {
        if (max.signum() <= 0 || value.signum() <= 0) {
            return 0;
        }
        int pct = value.multiply(BigDecimal.valueOf(100)).divide(max, 0, java.math.RoundingMode.HALF_UP).intValue();
        return Math.max(pct, 3); // keep tiny non-zero values visible
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}

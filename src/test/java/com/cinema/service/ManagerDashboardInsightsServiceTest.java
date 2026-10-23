package com.cinema.service;

import com.cinema.dao.RevenueReportDao;
import com.cinema.model.DashboardRevenueDay;
import com.cinema.model.report.DailyRevenueItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManagerDashboardInsightsServiceTest {

    @Test
    void weeklyRevenueFillsMissingDaysAndScalesBars() {
        RevenueReportDao dao = mock(RevenueReportDao.class);
        LocalDate today = LocalDate.of(2026, 10, 24);
        DailyRevenueItem todayRow = new DailyRevenueItem();
        todayRow.setDate(today);
        todayRow.setTicketRevenue(new BigDecimal("200000"));
        todayRow.setFbRevenue(new BigDecimal("50000"));
        when(dao.getDailyRevenue(any(), any())).thenReturn(List.of(todayRow));

        List<DashboardRevenueDay> days = new ManagerDashboardInsightsService(dao).buildWeeklyRevenue(today);

        assertEquals(7, days.size());
        assertEquals(today.minusDays(6), days.get(0).getDate());
        DashboardRevenueDay last = days.get(6);
        assertTrue(last.isToday());
        assertEquals("Today", last.getLabel());
        assertEquals(100, last.getTicketHeightPercent());
        assertEquals(25, last.getFbHeightPercent());
        assertEquals(0, days.get(0).getTicketHeightPercent());
        assertEquals(new BigDecimal("250000"), last.getTotalRevenue());
    }

    @Test
    void weeklyRevenueHandlesNoData() {
        RevenueReportDao dao = mock(RevenueReportDao.class);
        List<DashboardRevenueDay> days = new ManagerDashboardInsightsService(dao).buildWeeklyRevenue(LocalDate.now());
        assertEquals(7, days.size());
        assertTrue(days.stream().allMatch(d -> d.getTicketHeightPercent() == 0 && d.getFbHeightPercent() == 0));
    }
}

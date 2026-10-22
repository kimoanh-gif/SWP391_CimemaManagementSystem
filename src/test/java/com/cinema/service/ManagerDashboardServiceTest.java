package com.cinema.service;

import com.cinema.model.RevenueChart;
import com.cinema.model.RevenueDay;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagerDashboardServiceTest {

    @Test
    void niceCeilingRoundsUpToReadableSteps() {
        assertEquals(200_000_000, ManagerDashboardService.niceCeiling(148_650_000));
        assertEquals(250_000, ManagerDashboardService.niceCeiling(210_000));
        assertEquals(1_000_000, ManagerDashboardService.niceCeiling(0));
    }

    @Test
    void compactFormatsMillionsAndThousands() {
        assertEquals("148.65M", ManagerDashboardService.compact(148_650_000));
        assertEquals("25K", ManagerDashboardService.compact(25_000));
        assertEquals("1.5B", ManagerDashboardService.compact(1_500_000_000));
    }

    @Test
    void chartHeightsAreRelativeToTheAxisMaximum() {
        LocalDate day = LocalDate.of(2026, 10, 22);
        Map<String, RevenueDay> points = new LinkedHashMap<>();
        points.put("Mon", new RevenueDay(day, new BigDecimal("100000000"), new BigDecimal("50000000")));
        points.put("Today", new RevenueDay(day, BigDecimal.ZERO, BigDecimal.ZERO));

        RevenueChart chart = ManagerDashboardService.buildChart("7d", points,
                Map.of("Mon", false, "Today", true), Map.of("Mon", "Mon", "Today", "Thu"));

        List<RevenueChart.Bar> bars = chart.getBars();
        assertEquals(100.0, bars.get(0).getTicketHeight());
        assertEquals(50.0, bars.get(0).getFnbHeight());
        assertTrue(bars.get(1).isToday());
        assertEquals(67, chart.getTicketSharePercent());
        assertEquals(List.of("100M", "75M", "50M", "25M", "0"), chart.getAxisLabels());
    }
}

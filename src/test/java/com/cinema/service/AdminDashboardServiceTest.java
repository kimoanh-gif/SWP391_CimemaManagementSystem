package com.cinema.service;

import com.cinema.dao.AdminDashboardDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class AdminDashboardServiceTest {
    private AdminDashboardDao dao;
    private AdminDashboardService service;
    private final LocalDate today=LocalDate.of(2026,10,23);
    @BeforeEach void setup() {
        dao=mock(AdminDashboardDao.class);
        service=new AdminDashboardService(dao,Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"),ZoneId.of("Asia/Ho_Chi_Minh")));
        when(dao.summary(any(),any())).thenReturn(Map.of("revenue",new BigDecimal("125000"),"tickets",2));
        when(dao.daily(any(),any())).thenReturn(List.of());
    }
    @Test void emptyWeekContainsSevenRealZeroDays() {
        var data=service.load();
        assertEquals(BigDecimal.ZERO,data.get("weekTotal"));
        var days=(List<Map<String,Object>>)data.get("days");
        assertEquals(7,days.size());
        assertEquals(today.minusDays(6),days.get(0).get("day"));
        assertEquals(today,days.get(6).get("day"));
        assertTrue(days.stream().allMatch(d->BigDecimal.ZERO.equals(d.get("height"))));
        assertEquals(new BigDecimal("125000"),data.get("revenue"));
    }
    @Test void computesChartScaleAndTotalWithoutInventingMissingRevenue() {
        when(dao.daily(any(),any())).thenReturn(List.of(
                Map.of("day",java.sql.Date.valueOf(today.minusDays(2)),"amount",new BigDecimal("50000")),
                Map.of("day",java.sql.Date.valueOf(today),"amount",new BigDecimal("100000"))));
        var data=service.load();
        var days=(List<Map<String,Object>>)data.get("days");
        assertEquals(new BigDecimal("150000"),data.get("weekTotal"));
        assertEquals(new BigDecimal("50.00"),days.get(4).get("height"));
        assertEquals(new BigDecimal("100.00"),days.get(6).get("height"));
        assertEquals(true,days.get(6).get("peak"));
        assertEquals(BigDecimal.ZERO,days.get(5).get("amount"));
    }
    @Test void queriesUseVietnamDayBoundariesAndCurrentTime() {
        service.load();
        verify(dao).summary(today.atStartOfDay(),today.plusDays(1).atStartOfDay());
        verify(dao).daily(today.minusDays(6).atStartOfDay(),today.plusDays(1).atStartOfDay());
        verify(dao).topMovies(today.minusDays(6).atStartOfDay(),today.plusDays(1).atStartOfDay());
        verify(dao).upcoming(today.atTime(17,0),today.plusDays(1).atStartOfDay());
    }
    @Test void databaseFailureDoesNotBecomeFakeZeroStatistics() {
        when(dao.summary(any(),any())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("unavailable"));
        assertThrows(org.springframework.dao.DataAccessException.class,()->service.load());
    }
}

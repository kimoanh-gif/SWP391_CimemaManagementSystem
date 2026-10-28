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
    private final LocalDate today = LocalDate.of(2026,10,23);

    @BeforeEach void setup() {
        dao = mock(AdminDashboardDao.class);
        service = new AdminDashboardService(dao, Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));
        when(dao.summary(any(),any())).thenReturn(Map.of("revenue",new BigDecimal("125000"),"tickets",2));
        when(dao.revenue(any(),any())).thenReturn(BigDecimal.ZERO);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String,Object>> bars(Map<String,Object> data) {
        return (List<Map<String,Object>>) data.get("chartBars");
    }

    @Test void emptyDaysAreZeroAndUnknownSelectionFallsBackToDay() {
        var data = service.load("invalid");
        assertEquals("day", data.get("period"));
        assertEquals(BigDecimal.ZERO, data.get("chartTotal"));
        assertEquals(7, bars(data).size());
        assertEquals(today.minusDays(6), bars(data).get(0).get("start"));
        assertTrue(bars(data).stream().allMatch(bar -> BigDecimal.ZERO.equals(bar.get("height"))));
        assertEquals(new BigDecimal("125000"), data.get("revenue"));
    }

    @Test void databaseAmountsDetermineTotalAndBarScale() {
        when(dao.revenue(today.minusDays(2).atStartOfDay(), today.minusDays(1).atStartOfDay())).thenReturn(new BigDecimal("50000"));
        when(dao.revenue(today.atStartOfDay(), today.plusDays(1).atStartOfDay())).thenReturn(new BigDecimal("100000"));
        var data = service.load();
        assertEquals(new BigDecimal("150000"), data.get("chartTotal"));
        assertEquals(new BigDecimal("50.00"), bars(data).get(4).get("height"));
        assertEquals(new BigDecimal("100.00"), bars(data).get(6).get("height"));
        assertEquals(true, bars(data).get(6).get("peak"));
    }

    @Test void weeksStartMondayAndCurrentWeekStopsAtToday() {
        var data = service.load("week");
        assertEquals(8, bars(data).size());
        assertEquals(LocalDate.of(2026,10,19), bars(data).get(7).get("start"));
        verify(dao).revenue(LocalDate.of(2026,10,19).atStartOfDay(), today.plusDays(1).atStartOfDay());
    }

    @Test void monthsAndQuartersIncludeEmptyFutureBucketsWithoutQueryingFuturePayments() {
        var months = service.load("month");
        assertEquals(12, bars(months).size());
        assertEquals(BigDecimal.ZERO, bars(months).get(11).get("amount"));
        verify(dao).revenue(LocalDate.of(2026,10,1).atStartOfDay(), today.plusDays(1).atStartOfDay());
        verify(dao, never()).revenue(LocalDate.of(2026,11,1).atStartOfDay(), LocalDate.of(2026,12,1).atStartOfDay());
        var quarters = service.load("quarter");
        assertEquals(4, bars(quarters).size());
        assertEquals(LocalDate.of(2026,10,1), bars(quarters).get(3).get("start"));
        assertEquals(LocalDate.of(2026,12,31), bars(quarters).get(3).get("end"));
    }

    @Test void yearsCrossCalendarBoundariesAndIncludeLeapYear() {
        var data = service.load("year");
        assertEquals(5, bars(data).size());
        assertEquals(LocalDate.of(2022,1,1), bars(data).get(0).get("start"));
        verify(dao).revenue(LocalDate.of(2024,1,1).atStartOfDay(), LocalDate.of(2025,1,1).atStartOfDay());
        verify(dao).revenue(LocalDate.of(2026,1,1).atStartOfDay(), today.plusDays(1).atStartOfDay());
    }

    @Test void databaseFailureDoesNotBecomeFakeZeroStatistics() {
        when(dao.revenue(any(),any())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("offline"));
        assertThrows(org.springframework.dao.DataAccessException.class, () -> service.load());
    }

    @Test void customMonthsClipFirstAndLastBucketAndIncludeLastDay() {
        when(dao.daily(any(),any())).thenReturn(List.of(
                Map.of("day", java.sql.Date.valueOf("2026-02-28"), "amount", new BigDecimal("10")),
                Map.of("day", java.sql.Date.valueOf("2026-03-05"), "amount", new BigDecimal("20"))));
        var data = service.load("month", "2026-02-15", "2026-03-05");
        verify(dao).daily(LocalDate.of(2026,2,15).atStartOfDay(), LocalDate.of(2026,3,6).atStartOfDay());
        assertEquals(2, bars(data).size());
        assertEquals(LocalDate.of(2026,2,15), bars(data).get(0).get("start"));
        assertEquals(LocalDate.of(2026,2,28), bars(data).get(0).get("end"));
        assertEquals(LocalDate.of(2026,3,5), bars(data).get(1).get("end"));
        assertEquals(new BigDecimal("30"), data.get("chartTotal"));
        verify(dao, never()).revenue(any(),any());
    }

    @Test void rejectsInvalidRangesBeforeDatabaseQueries() {
        assertThrows(IllegalArgumentException.class, () -> service.load("day", "2026-03-02", "2026-03-01"));
        assertThrows(IllegalArgumentException.class, () -> service.load("day", "bad", "2026-03-01"));
        assertThrows(IllegalArgumentException.class, () -> service.load("day", "2026-03-01", null));
        assertThrows(IllegalArgumentException.class, () -> service.load("day", "2000-01-01", "2026-03-01"));
        verifyNoInteractions(dao);
    }

    @Test void customWeekCrossesYearAndKeepsCalendarMondayBoundary() {
        when(dao.daily(any(),any())).thenReturn(List.of());
        var data = service.load("week", "2025-12-31", "2026-01-06");
        assertEquals(2, bars(data).size());
        assertEquals(LocalDate.of(2026,1,4), bars(data).get(0).get("end"));
        assertEquals(LocalDate.of(2026,1,5), bars(data).get(1).get("start"));
        assertEquals(LocalDate.of(2026,1,6), data.get("chartEnd"));
    }

    @Test void dateRangeStaysTheSameAcrossGroupingChoices() {
        when(dao.daily(any(), any())).thenReturn(List.of());
        for (String period : List.of("day", "week", "month", "quarter", "year")) {
            var data = service.loadFiltered(period, "last30", "bad", "bad");
            assertEquals(today.minusDays(29), data.get("chartStart"));
            assertEquals(today, data.get("chartEnd"));
            assertEquals("last30", data.get("range"));
        }
    }

    @Test void quickViewOwnsRangeAndIgnoresCustomDates() {
        for (var entry : Map.of("day", 7, "week", 8, "month", 12, "quarter", 4, "year", 5).entrySet()) {
            var data = service.loadView(entry.getKey(), "quick", "bad", "bad");
            assertEquals(entry.getValue(), bars(data).size());
            assertEquals("quick", data.get("range"));
            assertEquals(entry.getKey(), data.get("period"));
        }
    }

    @Test void dateSearchDefaultsToSevenDaysAndValidatesBeforeQuerying() {
        var defaults = service.loadDateRange(null, null);
        assertEquals(today.minusDays(6), defaults.get("chartStart"));
        assertEquals(today, defaults.get("chartEnd"));
        clearInvocations(dao);
        for (String[] dates : List.of(new String[]{"", ""}, new String[]{null, "2026-10-23"},
                new String[]{"2026-10-23", null}, new String[]{"2026-02-30", "2026-03-01"},
                new String[]{"2026-10-24", "2026-10-23"}, new String[]{"2000-01-01", "2026-01-01"},
                new String[]{"1899-12-31", "1900-01-01"}, new String[]{"2026-1-01", "2026-02-01"})) {
            assertThrows(IllegalArgumentException.class, () -> service.loadDateRange(dates[0], dates[1]));
        }
        verifyNoInteractions(dao);
        var sameDay = service.loadDateRange("2024-02-29", "2024-02-29");
        assertEquals(1, bars(sameDay).size());
        assertEquals("day", sameDay.get("period"));
        assertEquals("year", service.loadDateRange("2016-10-23", "2026-10-23").get("period"));
    }

    @Test void customViewIgnoresSelectedPeriodAndGroupsByRangeLength() {
        when(dao.daily(any(), any())).thenReturn(List.of());
        for (var entry : Map.of("2024-01-31", "day", "2024-02-01", "week",
                "2024-03-30", "week", "2024-03-31", "month",
                "2025-12-31", "month", "2026-01-01", "quarter",
                "2028-12-31", "quarter", "2029-01-01", "year").entrySet()) {
            var data = service.loadView("invalid", "custom", "2024-01-01", entry.getKey());
            assertEquals(entry.getValue(), data.get("period"), entry.getKey());
            assertEquals(LocalDate.of(2024, 1, 1), data.get("chartStart"));
            assertEquals(LocalDate.parse(entry.getKey()), data.get("chartEnd"));
        }
        assertThrows(IllegalArgumentException.class, () -> service.loadView("day", "custom", null, null));
        assertThrows(IllegalArgumentException.class, () -> service.loadView("day", "custom", "2025-01-02", "2025-01-01"));
        assertThrows(IllegalArgumentException.class, () -> service.loadView("day", "custom", "2000-01-01", "2025-01-01"));
    }

    @Test void customRangeSupportsMultipleYearsWithDifferentGroups() {
        when(dao.daily(any(), any())).thenReturn(List.of());
        for (var entry : Map.of("month", 36, "quarter", 12, "year", 3).entrySet()) {
            var data = service.loadFiltered(entry.getKey(), "custom", "2023-01-01", "2025-12-31");
            assertEquals(entry.getValue(), bars(data).size());
            assertEquals(LocalDate.of(2023, 1, 1), data.get("chartStart"));
            assertEquals(LocalDate.of(2025, 12, 31), data.get("chartEnd"));
        }
    }

    @Test void calendarPresetsEndTodayAndCustomDatesAreRequired() {
        when(dao.daily(any(), any())).thenReturn(List.of());
        for (var entry : Map.of("month", LocalDate.of(2026, 10, 1),
                "quarter", LocalDate.of(2026, 10, 1), "year", LocalDate.of(2026, 1, 1)).entrySet()) {
            var data = service.loadFiltered("month", entry.getKey(), null, null);
            assertEquals(entry.getValue(), data.get("chartStart"));
            assertEquals(today, data.get("chartEnd"));
        }
        assertThrows(IllegalArgumentException.class, () -> service.loadFiltered("day", "custom", null, null));
        assertThrows(IllegalArgumentException.class, () -> service.loadFiltered("year", "custom", "2000-01-01", "2026-01-01"));
    }
}

package com.cinema.service;

import com.cinema.dao.RevenueReportDao;
import com.cinema.model.report.DailyRevenueItem;
import com.cinema.model.report.MovieRevenueItem;
import com.cinema.model.report.RevenueReportData;
import com.cinema.model.report.RevenueReportRequest;
import com.cinema.model.report.RevenueReportSummary;
import com.cinema.model.report.TransactionRevenueItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueReportServiceTest {

    @Mock
    private RevenueReportDao revenueReportDao;

    private RevenueReportService revenueReportService;

    @BeforeEach
    void setUp() {
        revenueReportService = new RevenueReportService(revenueReportDao);
    }

    @Test
    @DisplayName("resolveAndValidateDates: Correctly resolves 'today' preset")
    void testResolveDates_Today() {
        RevenueReportRequest req = new RevenueReportRequest();
        req.setPreset("today");

        revenueReportService.resolveAndValidateDates(req);

        LocalDate today = LocalDate.now();
        assertEquals(today, req.getStartDate());
        assertEquals(today, req.getEndDate());
    }

    @Test
    @DisplayName("resolveAndValidateDates: Correctly resolves 'last7days' preset")
    void testResolveDates_Last7Days() {
        RevenueReportRequest req = new RevenueReportRequest();
        req.setPreset("last7days");

        revenueReportService.resolveAndValidateDates(req);

        LocalDate today = LocalDate.now();
        assertEquals(today.minusDays(6), req.getStartDate());
        assertEquals(today, req.getEndDate());
    }

    @Test
    @DisplayName("resolveAndValidateDates: Throws IllegalArgumentException when startDate > endDate")
    void testResolveDates_InvalidRange() {
        RevenueReportRequest req = new RevenueReportRequest();
        req.setPreset("custom");
        req.setStartDate(LocalDate.of(2026, 10, 20));
        req.setEndDate(LocalDate.of(2026, 10, 10));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> revenueReportService.resolveAndValidateDates(req));

        assertEquals("Ngày bắt đầu không được lớn hơn ngày kết thúc.", ex.getMessage());
    }

    @Test
    @DisplayName("getReportData: Aggregates summary, daily, movie, and transaction data")
    void testGetReportData_Success() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);

        RevenueReportSummary summary = new RevenueReportSummary();
        summary.setTotalRevenue(new BigDecimal("1000000"));
        summary.setTicketRevenue(new BigDecimal("800000"));
        summary.setFbRevenue(new BigDecimal("200000"));
        summary.setTotalTickets(10);
        summary.setTotalPaidBookings(4);

        DailyRevenueItem daily = new DailyRevenueItem(start, new BigDecimal("500000"),
                new BigDecimal("400000"), new BigDecimal("100000"), BigDecimal.ZERO, 5, 2);

        MovieRevenueItem movie = new MovieRevenueItem(1, "Avatar", 3, 5,
                new BigDecimal("400000"), new BigDecimal("100000"), new BigDecimal("500000"));

        when(revenueReportDao.getSummary(start, end)).thenReturn(summary);
        when(revenueReportDao.getDailyRevenue(start, end)).thenReturn(List.of(daily));
        when(revenueReportDao.getMovieRevenue(start, end)).thenReturn(List.of(movie));
        when(revenueReportDao.getTransactions(start, end)).thenReturn(Collections.emptyList());

        RevenueReportRequest req = new RevenueReportRequest();
        req.setPreset("custom");
        req.setStartDate(start);
        req.setEndDate(end);

        RevenueReportData data = revenueReportService.getReportData(req);

        assertNotNull(data);
        assertEquals(new BigDecimal("1000000"), data.getSummary().getTotalRevenue());
        assertEquals(1, data.getDailyItems().size());
        assertEquals(1, data.getMovieItems().size());
    }

    @Test
    @DisplayName("generateCsvReport: Successfully generates UTF-8 CSV with BOM for Excel")
    void testGenerateCsvReport_Success() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 19);

        RevenueReportSummary summary = new RevenueReportSummary();
        summary.setTotalRevenue(new BigDecimal("2000000"));
        summary.setTicketRevenue(new BigDecimal("1500000"));
        summary.setFbRevenue(new BigDecimal("500000"));
        summary.setTotalTickets(15);
        summary.setTotalPaidBookings(5);

        DailyRevenueItem daily = new DailyRevenueItem(start, new BigDecimal("2000000"),
                new BigDecimal("1500000"), new BigDecimal("500000"), BigDecimal.ZERO, 15, 5);

        when(revenueReportDao.getSummary(start, end)).thenReturn(summary);
        when(revenueReportDao.getDailyRevenue(start, end)).thenReturn(List.of(daily));
        when(revenueReportDao.getMovieRevenue(start, end)).thenReturn(Collections.emptyList());
        when(revenueReportDao.getTransactions(start, end)).thenReturn(Collections.emptyList());

        RevenueReportRequest req = new RevenueReportRequest();
        req.setPreset("custom");
        req.setStartDate(start);
        req.setEndDate(end);
        req.setReportType("DAILY");

        byte[] csvBytes = revenueReportService.generateCsvReport(req);

        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 3);
        // Verify UTF-8 BOM bytes
        assertEquals((byte) 0xEF, csvBytes[0]);
        assertEquals((byte) 0xBB, csvBytes[1]);
        assertEquals((byte) 0xBF, csvBytes[2]);

        String text = new String(csvBytes, 3, csvBytes.length - 3, StandardCharsets.UTF_8);
        assertTrue(text.contains("BÁO CÁO DOANH THU RẠP CHIẾU PHIM"));
        assertTrue(text.contains("Tổng doanh thu toàn hệ thống"));
        assertTrue(text.contains("2,000,000"));
    }

    @Test
    @DisplayName("generateCsvReport: Generates movie and transaction reports properly")
    void testGenerateCsvReport_MovieAndTransactions() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 19);

        TransactionRevenueItem tx = new TransactionRevenueItem();
        tx.setBookingCode("BK-ROYAL-001");
        tx.setCustomerName("Nguyen Van A");
        tx.setPhone("0912345678");
        tx.setMovieTitle("Avatar");
        tx.setRoomName("Hall 01");
        tx.setSeatLabels("A1, A2");
        tx.setTicketAmount(new BigDecimal("180000"));
        tx.setFbAmount(new BigDecimal("50000"));
        tx.setDiscountAmount(BigDecimal.ZERO);
        tx.setTotalAmount(new BigDecimal("230000"));
        tx.setPaymentMethod("VNPAY");
        tx.setBookingStatus("PAID");
        tx.setBookingTime(LocalDateTime.now());

        when(revenueReportDao.getSummary(start, end)).thenReturn(new RevenueReportSummary());
        when(revenueReportDao.getDailyRevenue(start, end)).thenReturn(Collections.emptyList());
        when(revenueReportDao.getMovieRevenue(start, end)).thenReturn(Collections.emptyList());
        when(revenueReportDao.getTransactions(start, end)).thenReturn(List.of(tx));

        RevenueReportRequest req = new RevenueReportRequest();
        req.setPreset("custom");
        req.setStartDate(start);
        req.setEndDate(end);
        req.setReportType("TRANSACTIONS");

        byte[] csvBytes = revenueReportService.generateCsvReport(req);
        String text = new String(csvBytes, 3, csvBytes.length - 3, StandardCharsets.UTF_8);

        assertTrue(text.contains("BK-ROYAL-001"));
        assertTrue(text.contains("Nguyen Van A"));
        assertTrue(text.contains("230,000"));
    }
}

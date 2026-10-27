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
import java.util.ArrayList;
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

    @Test
    @DisplayName("generateCsvReport: Escapes monetary values with commas so columns do not split (BUG-UC34-02)")
    void testGenerateCsvReport_Rfc4180EscapingAndColumnCount() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);

        RevenueReportSummary summary = new RevenueReportSummary();
        summary.setTotalRevenue(new BigDecimal("816000"));
        summary.setTicketRevenue(new BigDecimal("340000"));
        summary.setFbRevenue(new BigDecimal("476000"));
        summary.setDiscountAmount(BigDecimal.ZERO);
        summary.setTotalTickets(4);
        summary.setTotalPaidBookings(2);

        DailyRevenueItem daily = new DailyRevenueItem(start, new BigDecimal("816000"),
                new BigDecimal("340000"), new BigDecimal("476000"), BigDecimal.ZERO, 4, 2);

        MovieRevenueItem movie = new MovieRevenueItem(1, "Avatar: The Way of Water", 5, 4,
                new BigDecimal("340000"), new BigDecimal("476000"), new BigDecimal("816000"));

        TransactionRevenueItem tx = new TransactionRevenueItem();
        tx.setBookingCode("BK-001");
        tx.setCustomerName("Khach vang lai");
        tx.setPhone("0900000001");
        tx.setMovieTitle("Avatar: The Way of Water");
        tx.setRoomName("Cinema 01");
        tx.setSeatLabels("A2, A3");
        tx.setTicketAmount(new BigDecimal("170000"));
        tx.setFbAmount(new BigDecimal("238000"));
        tx.setDiscountAmount(BigDecimal.ZERO);
        tx.setTotalAmount(new BigDecimal("408000"));
        tx.setPaymentMethod("COUNTER");
        tx.setBookingStatus("CONFIRMED");

        when(revenueReportDao.getSummary(start, end)).thenReturn(summary);
        when(revenueReportDao.getDailyRevenue(start, end)).thenReturn(List.of(daily));
        when(revenueReportDao.getMovieRevenue(start, end)).thenReturn(List.of(movie));
        when(revenueReportDao.getTransactions(start, end)).thenReturn(List.of(tx));

        // 1. Verify DAILY report CSV: 7 columns & UTF-8 BOM
        RevenueReportRequest dailyReq = new RevenueReportRequest();
        dailyReq.setStartDate(start);
        dailyReq.setEndDate(end);
        dailyReq.setReportType("DAILY");
        byte[] dailyBytes = revenueReportService.generateCsvReport(dailyReq);
        assertEquals((byte) 0xEF, dailyBytes[0]);
        assertEquals((byte) 0xBB, dailyBytes[1]);
        assertEquals((byte) 0xBF, dailyBytes[2]);

        String dailyCsv = new String(dailyBytes, 3, dailyBytes.length - 3, StandardCharsets.UTF_8);
        assertTrue(dailyCsv.contains("\"340,000\""));
        assertTrue(dailyCsv.contains("\"476,000\""));
        assertTrue(dailyCsv.contains("\"816,000\""));

        String[] dailyLines = dailyCsv.split("\r?\n");
        String dailyHeader = null;
        String dailyDataLine = null;
        for (String line : dailyLines) {
            if (line.contains("Doanh Thu") && (line.contains("Vé") || line.contains("V")) && line.contains("F&B")) {
                dailyHeader = line;
            } else if (line.contains("01/10/2026") && line.contains("340,000")) {
                dailyDataLine = line;
            }
        }
        assertNotNull(dailyHeader, "Daily header line should be found");
        assertNotNull(dailyDataLine, "Daily data line should be found");
        assertEquals(7, parseCsvLine(dailyHeader).size());
        List<String> dailyCols = parseCsvLine(dailyDataLine);
        assertEquals(7, dailyCols.size(), "Daily data line must have exactly 7 columns");
        assertEquals("01/10/2026", dailyCols.get(0));
        assertEquals("340,000", dailyCols.get(1));
        assertEquals("476,000", dailyCols.get(2));
        assertEquals("0", dailyCols.get(3));
        assertEquals("816,000", dailyCols.get(4));
        assertEquals("4", dailyCols.get(5));
        assertEquals("2", dailyCols.get(6));

        // 2. Verify MOVIE report CSV: 7 columns & UTF-8 BOM
        RevenueReportRequest movieReq = new RevenueReportRequest();
        movieReq.setStartDate(start);
        movieReq.setEndDate(end);
        movieReq.setReportType("MOVIE");
        byte[] movieBytes = revenueReportService.generateCsvReport(movieReq);
        assertEquals((byte) 0xEF, movieBytes[0]);
        assertEquals((byte) 0xBB, movieBytes[1]);
        assertEquals((byte) 0xBF, movieBytes[2]);

        String movieCsv = new String(movieBytes, 3, movieBytes.length - 3, StandardCharsets.UTF_8);
        String[] movieLines = movieCsv.split("\r?\n");
        String movieHeader = null;
        String movieDataLine = null;
        for (String line : movieLines) {
            if (line.contains("Phim") && (line.contains("Chiếu") || line.contains("Bán") || line.contains("Doanh Thu"))) {
                movieHeader = line;
            } else if (line.contains("Avatar") && line.contains("340,000")) {
                movieDataLine = line;
            }
        }
        assertNotNull(movieHeader, "Movie header line should be found");
        assertNotNull(movieDataLine, "Movie data line should be found");
        assertEquals(7, parseCsvLine(movieHeader).size());
        List<String> movieCols = parseCsvLine(movieDataLine);
        assertEquals(7, movieCols.size(), "Movie data line must have exactly 7 columns");
        assertEquals("1", movieCols.get(0));
        assertEquals("Avatar: The Way of Water", movieCols.get(1));
        assertEquals("5", movieCols.get(2));
        assertEquals("4", movieCols.get(3));
        assertEquals("340,000", movieCols.get(4));
        assertEquals("476,000", movieCols.get(5));
        assertEquals("816,000", movieCols.get(6));

        // 3. Verify TRANSACTIONS report CSV: 14 columns & UTF-8 BOM
        RevenueReportRequest txReq = new RevenueReportRequest();
        txReq.setStartDate(start);
        txReq.setEndDate(end);
        txReq.setReportType("TRANSACTIONS");
        byte[] txBytes = revenueReportService.generateCsvReport(txReq);
        assertEquals((byte) 0xEF, txBytes[0]);
        assertEquals((byte) 0xBB, txBytes[1]);
        assertEquals((byte) 0xBF, txBytes[2]);

        String txCsv = new String(txBytes, 3, txBytes.length - 3, StandardCharsets.UTF_8);
        String[] txLines = txCsv.split("\r?\n");
        String txHeader = null;
        String txDataLine = null;
        for (String line : txLines) {
            if ((line.contains("Mã Đơn") || line.contains("Thời Gian Đặt")) && line.contains("Khách Hàng")) {
                txHeader = line;
            } else if (line.contains("BK-001")) {
                txDataLine = line;
            }
        }
        assertNotNull(txHeader, "Transaction header line should be found");
        assertNotNull(txDataLine, "Transaction data line should be found");
        assertEquals(14, parseCsvLine(txHeader).size());
        List<String> txCols = parseCsvLine(txDataLine);
        assertEquals(14, txCols.size(), "Transaction data line must have exactly 14 columns");
        assertEquals("BK-001", txCols.get(0));
        assertEquals("Avatar: The Way of Water", txCols.get(4));
        assertEquals("Cinema 01", txCols.get(5));
        assertEquals("A2, A3", txCols.get(7));
        assertEquals("170,000", txCols.get(8));
        assertEquals("238,000", txCols.get(9));
        assertEquals("0", txCols.get(10));
        assertEquals("408,000", txCols.get(11));
        assertEquals("COUNTER", txCols.get(12));
        assertEquals("CONFIRMED", txCols.get(13));
    }

    private List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens;
    }
}

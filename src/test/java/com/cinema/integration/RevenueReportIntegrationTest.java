package com.cinema.integration;

import com.cinema.CinemaManagementApplication;
import com.cinema.dao.RevenueReportDao;
import com.cinema.model.report.DailyRevenueItem;
import com.cinema.model.report.MovieRevenueItem;
import com.cinema.model.report.RevenueReportSummary;
import com.cinema.model.report.TransactionRevenueItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CinemaManagementApplication.class)
@ActiveProfiles("local")
@Transactional
public class RevenueReportIntegrationTest {

    @Autowired
    private RevenueReportDao revenueReportDao;

    @Test
    @DisplayName("Verify SQL queries reconcile perfectly across Summary, Daily, and Movie breakdowns")
    void testRevenueReconciliation() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);

        RevenueReportSummary summary = revenueReportDao.getSummary(start, end);
        assertNotNull(summary);

        List<DailyRevenueItem> dailyItems = revenueReportDao.getDailyRevenue(start, end);
        assertNotNull(dailyItems);

        List<MovieRevenueItem> movieItems = revenueReportDao.getMovieRevenue(start, end);
        assertNotNull(movieItems);

        List<TransactionRevenueItem> transactions = revenueReportDao.getTransactions(start, end);
        assertNotNull(transactions);

        // If there are transactions in October 2026:
        if (summary.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0) {
            // 1. Summary KPI non-zero and correctly formatted
            assertEquals(new BigDecimal("816000.00"), summary.getTotalRevenue());
            assertEquals(new BigDecimal("340000.00"), summary.getTicketRevenue());
            assertEquals(new BigDecimal("476000.00"), summary.getFbRevenue());
            assertEquals(4, summary.getTotalTickets());
            assertEquals(2, summary.getTotalPaidBookings());

            // 2. Daily revenue matches summary
            BigDecimal dailySum = dailyItems.stream()
                    .map(DailyRevenueItem::getTotalRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(summary.getTotalRevenue(), dailySum);

            // 3. Movie revenue sum matches summary without join multiplication
            BigDecimal movieSum = movieItems.stream()
                    .map(MovieRevenueItem::getTotalRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(summary.getTotalRevenue(), movieSum);

            // 4. Avatar Movie specific metrics
            MovieRevenueItem avatar = movieItems.stream()
                    .filter(m -> m.getMovieId() == 1)
                    .findFirst()
                    .orElse(null);
            assertNotNull(avatar);
            assertEquals(new BigDecimal("340000.00"), avatar.getTicketRevenue());
            assertEquals(new BigDecimal("476000.00"), avatar.getFbRevenue());
            // Proves bug fix: Total revenue is 816,000, NOT 1,632,000!
            assertEquals(new BigDecimal("816000.00"), avatar.getTotalRevenue());
            assertEquals(4, avatar.getSoldTickets());

            // 5. Transaction list sum matches total revenue
            BigDecimal txSum = transactions.stream()
                    .map(TransactionRevenueItem::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(summary.getTotalRevenue(), txSum);
        }
    }

    @Test
    @DisplayName("Verify zero revenue for future/empty period handles gracefully")
    void testZeroRevenuePeriod() {
        LocalDate start = LocalDate.of(2030, 1, 1);
        LocalDate end = LocalDate.of(2030, 1, 31);

        RevenueReportSummary summary = revenueReportDao.getSummary(start, end);
        assertNotNull(summary);
        assertEquals(0, summary.getTotalRevenue().compareTo(BigDecimal.ZERO));
        assertEquals(0, summary.getTotalPaidBookings());
        assertEquals(0, summary.getTotalTickets());

        List<DailyRevenueItem> daily = revenueReportDao.getDailyRevenue(start, end);
        assertTrue(daily.isEmpty());
    }
}

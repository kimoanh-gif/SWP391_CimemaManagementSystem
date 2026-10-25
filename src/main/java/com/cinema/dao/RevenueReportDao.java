package com.cinema.dao;

import com.cinema.model.report.DailyRevenueItem;
import com.cinema.model.report.MovieRevenueItem;
import com.cinema.model.report.RevenueReportSummary;
import com.cinema.model.report.TransactionRevenueItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DAO querying existing database tables for Revenue Reports (Screen 47 & 49).
 * Strictly complies with BR-22, BR-24, BR-25, BR-37, BR-41:
 * Only bookings with Status IN ('PAID', 'CONFIRMED', 'COMPLETED') are recognized as revenue.
 */
@Repository
public class RevenueReportDao {

    private final JdbcTemplate jdbcTemplate;

    public RevenueReportDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Compute top KPI summary metrics for the given date range.
     */
    public RevenueReportSummary getSummary(LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT
                    COALESCE(SUM(b.TotalAmount), 0) AS TotalRevenue,
                    COALESCE(SUM(b.DiscountAmount), 0) AS TotalDiscount,
                    COALESCE(COUNT(DISTINCT b.BookingID), 0) AS TotalPaidBookings
                FROM dbo.Booking b
                WHERE UPPER(b.[Status]) IN ('PAID', 'CONFIRMED', 'COMPLETED')
                  AND CAST(b.BookingTime AS DATE) BETWEEN ? AND ?
                """;

        RevenueReportSummary summary = new RevenueReportSummary();

        try {
            jdbcTemplate.query(sql, rs -> {
                BigDecimal totalRev = rs.getBigDecimal("TotalRevenue");
                BigDecimal discount = rs.getBigDecimal("TotalDiscount");
                summary.setTotalRevenue(totalRev != null ? totalRev : BigDecimal.ZERO);
                summary.setDiscountAmount(discount != null ? discount : BigDecimal.ZERO);
                summary.setTotalPaidBookings(rs.getInt("TotalPaidBookings"));
            }, Date.valueOf(startDate), Date.valueOf(endDate));

            // Sum of F&B Revenue for paid bookings
            String fbSql = """
                    SELECT COALESCE(SUM(bfb.Quantity * bfb.UnitPrice), 0) AS FbRevenue
                    FROM dbo.BookingFoodBeverage bfb
                    JOIN dbo.Booking b ON b.BookingID = bfb.BookingID
                    WHERE UPPER(b.[Status]) IN ('PAID', 'CONFIRMED', 'COMPLETED')
                      AND CAST(b.BookingTime AS DATE) BETWEEN ? AND ?
                    """;
            BigDecimal fbRevenue = jdbcTemplate.queryForObject(fbSql, BigDecimal.class,
                    Date.valueOf(startDate), Date.valueOf(endDate));
            summary.setFbRevenue(fbRevenue != null ? fbRevenue : BigDecimal.ZERO);

            // Sum of Ticket Seats and Ticket Revenue for paid bookings
            String ticketSql = """
                    SELECT
                        COALESCE(COUNT(bs.BookingSeatID), 0) AS TotalTickets,
                        COALESCE(SUM(bs.UnitPrice), 0) AS TicketRevenue
                    FROM dbo.BookingSeat bs
                    JOIN dbo.Booking b ON b.BookingID = bs.BookingID
                    WHERE UPPER(b.[Status]) IN ('PAID', 'CONFIRMED', 'COMPLETED')
                      AND CAST(b.BookingTime AS DATE) BETWEEN ? AND ?
                    """;
            jdbcTemplate.query(ticketSql, rs -> {
                summary.setTotalTickets(rs.getInt("TotalTickets"));
                BigDecimal seatRev = rs.getBigDecimal("TicketRevenue");
                summary.setTicketRevenue(seatRev != null ? seatRev : BigDecimal.ZERO);
            }, Date.valueOf(startDate), Date.valueOf(endDate));

            // In case bookings have tickets but no separate BookingSeat records (counter sales fallback)
            if (summary.getTicketRevenue().compareTo(BigDecimal.ZERO) == 0 && summary.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal derivedTicket = summary.getTotalRevenue().subtract(summary.getFbRevenue());
                if (derivedTicket.compareTo(BigDecimal.ZERO) > 0) {
                    summary.setTicketRevenue(derivedTicket);
                }
            }

        } catch (Exception ex) {
            // Safe fallback if tables are empty or query fails
            return new RevenueReportSummary();
        }

        return summary;
    }

    /**
     * Compute daily aggregated revenue breakdown.
     */
    public List<DailyRevenueItem> getDailyRevenue(LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT
                    CAST(b.BookingTime AS DATE) AS ReportDate,
                    COALESCE(SUM(b.TotalAmount), 0) AS TotalRevenue,
                    COALESCE(SUM(b.DiscountAmount), 0) AS DiscountAmount,
                    COALESCE(COUNT(DISTINCT b.BookingID), 0) AS BookingCount,
                    COALESCE((
                        SELECT SUM(bs.UnitPrice)
                        FROM dbo.BookingSeat bs
                        JOIN dbo.Booking b2 ON b2.BookingID = bs.BookingID
                        WHERE UPPER(b2.[Status]) IN ('PAID', 'CONFIRMED', 'COMPLETED')
                          AND CAST(b2.BookingTime AS DATE) = CAST(b.BookingTime AS DATE)
                    ), 0) AS TicketRevenue,
                    COALESCE((
                        SELECT COUNT(bs2.BookingSeatID)
                        FROM dbo.BookingSeat bs2
                        JOIN dbo.Booking b3 ON b3.BookingID = bs2.BookingID
                        WHERE UPPER(b3.[Status]) IN ('PAID', 'CONFIRMED', 'COMPLETED')
                          AND CAST(b3.BookingTime AS DATE) = CAST(b.BookingTime AS DATE)
                    ), 0) AS TicketCount,
                    COALESCE((
                        SELECT SUM(bfb.Quantity * bfb.UnitPrice)
                        FROM dbo.BookingFoodBeverage bfb
                        JOIN dbo.Booking b4 ON b4.BookingID = bfb.BookingID
                        WHERE UPPER(b4.[Status]) IN ('PAID', 'CONFIRMED', 'COMPLETED')
                          AND CAST(b4.BookingTime AS DATE) = CAST(b.BookingTime AS DATE)
                    ), 0) AS FbRevenue
                FROM dbo.Booking b
                WHERE UPPER(b.[Status]) IN ('PAID', 'CONFIRMED', 'COMPLETED')
                  AND CAST(b.BookingTime AS DATE) BETWEEN ? AND ?
                GROUP BY CAST(b.BookingTime AS DATE)
                ORDER BY ReportDate DESC
                """;

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                DailyRevenueItem item = new DailyRevenueItem();
                Date d = rs.getDate("ReportDate");
                if (d != null) item.setDate(d.toLocalDate());
                item.setTotalRevenue(rs.getBigDecimal("TotalRevenue"));
                item.setDiscountAmount(rs.getBigDecimal("DiscountAmount"));
                item.setBookingCount(rs.getInt("BookingCount"));
                item.setTicketRevenue(rs.getBigDecimal("TicketRevenue"));
                item.setTicketCount(rs.getInt("TicketCount"));
                item.setFbRevenue(rs.getBigDecimal("FbRevenue"));
                return item;
            }, Date.valueOf(startDate), Date.valueOf(endDate));
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    /**
     * Compute movie sales performance for the given date range.
     */
    public List<MovieRevenueItem> getMovieRevenue(LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT
                    m.MovieID,
                    m.Title,
                    COUNT(DISTINCT st.ShowtimeID) AS ShowtimesCount,
                    COALESCE(COUNT(DISTINCT bs.BookingSeatID), 0) AS SoldTickets,
                    COALESCE(SUM(bs.UnitPrice), 0) AS TicketRevenue,
                    COALESCE((
                        SELECT SUM(bfb.Quantity * bfb.UnitPrice)
                        FROM dbo.BookingFoodBeverage bfb
                        JOIN dbo.Booking b_fb ON b_fb.BookingID = bfb.BookingID
                        JOIN dbo.Showtime st_fb ON st_fb.ShowtimeID = b_fb.ShowtimeID
                        WHERE st_fb.MovieID = m.MovieID
                          AND UPPER(b_fb.[Status]) IN ('PAID', 'CONFIRMED', 'COMPLETED')
                          AND CAST(b_fb.BookingTime AS DATE) BETWEEN ? AND ?
                    ), 0) AS FbRevenue,
                    COALESCE(SUM(b.TotalAmount), 0) AS TotalRevenue
                FROM dbo.Movie m
                JOIN dbo.Showtime st ON st.MovieID = m.MovieID
                LEFT JOIN dbo.Booking b ON b.ShowtimeID = st.ShowtimeID
                     AND UPPER(b.[Status]) IN ('PAID', 'CONFIRMED', 'COMPLETED')
                     AND CAST(b.BookingTime AS DATE) BETWEEN ? AND ?
                LEFT JOIN dbo.BookingSeat bs ON bs.BookingID = b.BookingID
                GROUP BY m.MovieID, m.Title
                HAVING COUNT(DISTINCT bs.BookingSeatID) > 0 OR COUNT(DISTINCT st.ShowtimeID) > 0
                ORDER BY TotalRevenue DESC, SoldTickets DESC
                """;

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                MovieRevenueItem item = new MovieRevenueItem();
                item.setMovieId(rs.getInt("MovieID"));
                item.setTitle(rs.getString("Title"));
                item.setShowtimesCount(rs.getInt("ShowtimesCount"));
                item.setSoldTickets(rs.getInt("SoldTickets"));
                item.setTicketRevenue(rs.getBigDecimal("TicketRevenue"));
                item.setFbRevenue(rs.getBigDecimal("FbRevenue"));
                item.setTotalRevenue(rs.getBigDecimal("TotalRevenue"));
                return item;
            }, Date.valueOf(startDate), Date.valueOf(endDate),
               Date.valueOf(startDate), Date.valueOf(endDate));
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    /**
     * Query detailed transactions list for reconciliation and export.
     */
    public List<TransactionRevenueItem> getTransactions(LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT
                    b.BookingID,
                    b.BookingCode,
                    COALESCE(a.FullName, N'Khách vãng lai') AS CustomerName,
                    COALESCE(a.Phone, N'-') AS Phone,
                    m.Title AS MovieTitle,
                    r.RoomName,
                    st.StartTime AS ShowtimeStartTime,
                    b.TotalAmount,
                    b.DiscountAmount,
                    b.BookingTime,
                    b.[Status] AS BookingStatus,
                    COALESCE((
                        SELECT STRING_AGG(CONCAT(s.RowLabel, s.SeatNumber), ', ')
                        FROM dbo.BookingSeat bs2
                        JOIN dbo.Seat s ON s.SeatID = bs2.SeatID
                        WHERE bs2.BookingID = b.BookingID
                    ), N'-') AS SeatLabels,
                    COALESCE((
                        SELECT SUM(bs3.UnitPrice)
                        FROM dbo.BookingSeat bs3
                        WHERE bs3.BookingID = b.BookingID
                    ), b.Subtotal) AS TicketAmount,
                    COALESCE((
                        SELECT SUM(bfb.Quantity * bfb.UnitPrice)
                        FROM dbo.BookingFoodBeverage bfb
                        WHERE bfb.BookingID = b.BookingID
                    ), 0) AS FbAmount,
                    COALESCE((
                        SELECT TOP 1 p.Method
                        FROM dbo.Payment p
                        WHERE p.BookingID = b.BookingID
                        ORDER BY p.PaymentID DESC
                    ), b.BookingType) AS PaymentMethod
                FROM dbo.Booking b
                JOIN dbo.Showtime st ON st.ShowtimeID = b.ShowtimeID
                JOIN dbo.Movie m ON m.MovieID = st.MovieID
                JOIN dbo.Room r ON r.RoomID = st.RoomID
                LEFT JOIN dbo.Customer c ON c.CustomerID = b.CustomerID
                LEFT JOIN dbo.Account a ON a.AccountID = c.AccountID
                WHERE UPPER(b.[Status]) IN ('PAID', 'CONFIRMED', 'COMPLETED')
                  AND CAST(b.BookingTime AS DATE) BETWEEN ? AND ?
                ORDER BY b.BookingTime DESC
                """;

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                TransactionRevenueItem item = new TransactionRevenueItem();
                item.setBookingId(rs.getInt("BookingID"));
                item.setBookingCode(rs.getString("BookingCode"));
                item.setCustomerName(rs.getString("CustomerName"));
                item.setPhone(rs.getString("Phone"));
                item.setMovieTitle(rs.getString("MovieTitle"));
                item.setRoomName(rs.getString("RoomName"));

                java.sql.Timestamp showtimeTs = rs.getTimestamp("ShowtimeStartTime");
                if (showtimeTs != null) item.setShowtimeStartTime(showtimeTs.toLocalDateTime());

                java.sql.Timestamp bookingTs = rs.getTimestamp("BookingTime");
                if (bookingTs != null) item.setBookingTime(bookingTs.toLocalDateTime());

                item.setSeatLabels(rs.getString("SeatLabels"));
                item.setTicketAmount(rs.getBigDecimal("TicketAmount"));
                item.setFbAmount(rs.getBigDecimal("FbAmount"));
                item.setDiscountAmount(rs.getBigDecimal("DiscountAmount"));
                item.setTotalAmount(rs.getBigDecimal("TotalAmount"));
                item.setPaymentMethod(rs.getString("PaymentMethod"));
                item.setBookingStatus(rs.getString("BookingStatus"));
                return item;
            }, Date.valueOf(startDate), Date.valueOf(endDate));
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }
}

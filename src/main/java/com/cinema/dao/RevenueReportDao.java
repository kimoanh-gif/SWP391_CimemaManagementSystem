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
import java.util.Collections;
import java.util.List;

/**
 * DAO querying existing database tables for Revenue Reports (Screen 47 & 49).
 * Strictly complies with BR-22, BR-24, BR-25, BR-37, BR-41:
 * Only confirmed bookings with successful payment (PaidAt) are recognized as collected cash revenue.
 * Guards against multiple payment attempts by deduplicating Payment records per BookingID.
 */
@Repository
public class RevenueReportDao {

    private final JdbcTemplate jdbcTemplate;

    /**
     * CTE to isolate confirmed bookings by their earliest settled payment timestamp (PaidAt).
     * Strictly uses schema-valid statuses: Payment.Status = 'PAID' (with PaidAt IS NOT NULL)
     * and Booking.Status = 'CONFIRMED'. Never substitutes CreatedAt for PaidAt.
     */
    private static final String PAID_BOOKINGS_CTE = """
            WITH paid_bookings AS (
                SELECT
                    p.BookingID,
                    MIN(p.PaidAt) AS PaidTime
                FROM dbo.Payment p
                JOIN dbo.Booking b ON b.BookingID = p.BookingID
                WHERE UPPER(p.[Status]) = 'PAID'
                  AND p.PaidAt IS NOT NULL
                  AND UPPER(b.[Status]) = 'CONFIRMED'
                GROUP BY p.BookingID
            )
            """;

    public RevenueReportDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Counts anomalous payment or booking records (e.g., PAID payment missing PaidAt,
     * or CONFIRMED booking without a settled PAID payment).
     */
    public int countAnomalousPaymentRecords() {
        String sql = """
                SELECT (
                    SELECT COUNT(*) FROM dbo.Payment p
                    WHERE UPPER(p.[Status]) = 'PAID' AND p.PaidAt IS NULL
                ) + (
                    SELECT COUNT(*) FROM dbo.Booking b
                    WHERE UPPER(b.[Status]) = 'CONFIRMED'
                      AND NOT EXISTS (
                          SELECT 1 FROM dbo.Payment p2
                          WHERE p2.BookingID = b.BookingID
                            AND UPPER(p2.[Status]) = 'PAID'
                            AND p2.PaidAt IS NOT NULL
                      )
                )
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Compute top KPI summary metrics for the given date range.
     */
    public RevenueReportSummary getSummary(LocalDate startDate, LocalDate endDate) {
        String sql = PAID_BOOKINGS_CTE + """
                SELECT
                    COALESCE(SUM(b.TotalAmount), 0) AS TotalRevenue,
                    COALESCE(SUM(b.DiscountAmount), 0) AS TotalDiscount,
                    COALESCE(COUNT(DISTINCT b.BookingID), 0) AS TotalPaidBookings
                FROM dbo.Booking b
                JOIN paid_bookings pb ON pb.BookingID = b.BookingID
                WHERE UPPER(b.[Status]) = 'CONFIRMED'
                  AND CAST(pb.PaidTime AS DATE) BETWEEN ? AND ?
                """;

        RevenueReportSummary summary = new RevenueReportSummary();

        jdbcTemplate.query(sql, rs -> {
            BigDecimal totalRev = rs.getBigDecimal("TotalRevenue");
            summary.setTotalRevenue(totalRev != null ? totalRev : BigDecimal.ZERO);
            BigDecimal totalDisc = rs.getBigDecimal("TotalDiscount");
            summary.setDiscountAmount(totalDisc != null ? totalDisc : BigDecimal.ZERO);
            summary.setTotalPaidBookings(rs.getInt("TotalPaidBookings"));
        }, Date.valueOf(startDate), Date.valueOf(endDate));

        // Sum of F&B Revenue for confirmed paid bookings (isolated from BookingSeat to prevent Cartesian multiplication)
        String fbSql = PAID_BOOKINGS_CTE + """
                SELECT COALESCE(SUM(bfb.Quantity * bfb.UnitPrice), 0) AS FbRevenue
                FROM dbo.BookingFoodBeverage bfb
                JOIN dbo.Booking b ON b.BookingID = bfb.BookingID
                JOIN paid_bookings pb ON pb.BookingID = b.BookingID
                WHERE UPPER(b.[Status]) = 'CONFIRMED'
                  AND CAST(pb.PaidTime AS DATE) BETWEEN ? AND ?
                """;
        BigDecimal fbRevenue = jdbcTemplate.queryForObject(fbSql, BigDecimal.class,
                Date.valueOf(startDate), Date.valueOf(endDate));
        summary.setFbRevenue(fbRevenue != null ? fbRevenue : BigDecimal.ZERO);

        // Sum of Booked Ticket Seats and Ticket Revenue for confirmed paid bookings (excluding RELEASED/HELD seats and CANCELLED tickets)
        String ticketSql = PAID_BOOKINGS_CTE + """
                SELECT
                    COALESCE(COUNT(bs.BookingSeatID), 0) AS TotalTickets,
                    COALESCE(SUM(bs.UnitPrice), 0) AS TicketRevenue
                FROM dbo.BookingSeat bs
                JOIN dbo.Booking b ON b.BookingID = bs.BookingID
                JOIN paid_bookings pb ON pb.BookingID = b.BookingID
                WHERE UPPER(b.[Status]) = 'CONFIRMED'
                  AND UPPER(bs.[Status]) = 'BOOKED'
                  AND NOT EXISTS (
                      SELECT 1 FROM dbo.Ticket t
                      WHERE t.BookingSeatID = bs.BookingSeatID
                        AND UPPER(t.TicketStatus) = 'CANCELLED'
                  )
                  AND CAST(pb.PaidTime AS DATE) BETWEEN ? AND ?
                """;
        jdbcTemplate.query(ticketSql, rs -> {
            summary.setTotalTickets(rs.getInt("TotalTickets"));
            BigDecimal seatRev = rs.getBigDecimal("TicketRevenue");
            summary.setTicketRevenue(seatRev != null ? seatRev : BigDecimal.ZERO);
        }, Date.valueOf(startDate), Date.valueOf(endDate));

        return summary;
    }

    /**
     * Compute daily aggregated revenue breakdown.
     */
    public List<DailyRevenueItem> getDailyRevenue(LocalDate startDate, LocalDate endDate) {
        String sql = PAID_BOOKINGS_CTE + """
                SELECT
                    CAST(pb.PaidTime AS DATE) AS ReportDate,
                    COALESCE(SUM(b.TotalAmount), 0) AS TotalRevenue,
                    COALESCE(SUM(b.DiscountAmount), 0) AS DiscountAmount,
                    COALESCE(COUNT(DISTINCT b.BookingID), 0) AS BookingCount,
                    COALESCE((
                        SELECT SUM(bs.UnitPrice)
                        FROM dbo.BookingSeat bs
                        JOIN dbo.Booking b2 ON b2.BookingID = bs.BookingID
                        JOIN paid_bookings pb2 ON pb2.BookingID = b2.BookingID
                        WHERE UPPER(b2.[Status]) = 'CONFIRMED'
                          AND UPPER(bs.[Status]) = 'BOOKED'
                          AND NOT EXISTS (
                              SELECT 1 FROM dbo.Ticket t
                              WHERE t.BookingSeatID = bs.BookingSeatID
                                AND UPPER(t.TicketStatus) = 'CANCELLED'
                          )
                          AND CAST(pb2.PaidTime AS DATE) = CAST(pb.PaidTime AS DATE)
                    ), 0) AS TicketRevenue,
                    COALESCE((
                        SELECT COUNT(bs2.BookingSeatID)
                        FROM dbo.BookingSeat bs2
                        JOIN dbo.Booking b3 ON b3.BookingID = bs2.BookingID
                        JOIN paid_bookings pb3 ON pb3.BookingID = b3.BookingID
                        WHERE UPPER(b3.[Status]) = 'CONFIRMED'
                          AND UPPER(bs2.[Status]) = 'BOOKED'
                          AND NOT EXISTS (
                              SELECT 1 FROM dbo.Ticket t2
                              WHERE t2.BookingSeatID = bs2.BookingSeatID
                                AND UPPER(t2.TicketStatus) = 'CANCELLED'
                          )
                          AND CAST(pb3.PaidTime AS DATE) = CAST(pb.PaidTime AS DATE)
                    ), 0) AS TicketCount,
                    COALESCE((
                        SELECT SUM(bfb.Quantity * bfb.UnitPrice)
                        FROM dbo.BookingFoodBeverage bfb
                        JOIN dbo.Booking b4 ON b4.BookingID = bfb.BookingID
                        JOIN paid_bookings pb4 ON pb4.BookingID = b4.BookingID
                        WHERE UPPER(b4.[Status]) = 'CONFIRMED'
                          AND CAST(pb4.PaidTime AS DATE) = CAST(pb.PaidTime AS DATE)
                    ), 0) AS FbRevenue
                FROM dbo.Booking b
                JOIN paid_bookings pb ON pb.BookingID = b.BookingID
                WHERE UPPER(b.[Status]) = 'CONFIRMED'
                  AND CAST(pb.PaidTime AS DATE) BETWEEN ? AND ?
                GROUP BY CAST(pb.PaidTime AS DATE)
                ORDER BY ReportDate DESC
                """;

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
    }

    /**
     * Compute movie sales performance for the given date range.
     */
    public List<MovieRevenueItem> getMovieRevenue(LocalDate startDate, LocalDate endDate) {
        String sql = PAID_BOOKINGS_CTE + """
                SELECT
                    m.MovieID,
                    m.Title,
                    COUNT(DISTINCT CASE WHEN CAST(st.StartTime AS DATE) BETWEEN ? AND ? THEN st.ShowtimeID END) AS ShowtimesCount,
                    COALESCE(COUNT(DISTINCT bs.BookingSeatID), 0) AS SoldTickets,
                    COALESCE(SUM(bs.UnitPrice), 0) AS TicketRevenue,
                    COALESCE((
                        SELECT SUM(bfb.Quantity * bfb.UnitPrice)
                        FROM dbo.BookingFoodBeverage bfb
                        JOIN dbo.Booking b_fb ON b_fb.BookingID = bfb.BookingID
                        JOIN paid_bookings pb_fb ON pb_fb.BookingID = b_fb.BookingID
                        JOIN dbo.Showtime st_fb ON st_fb.ShowtimeID = b_fb.ShowtimeID
                        WHERE st_fb.MovieID = m.MovieID
                          AND UPPER(b_fb.[Status]) = 'CONFIRMED'
                          AND CAST(pb_fb.PaidTime AS DATE) BETWEEN ? AND ?
                    ), 0) AS FbRevenue,
                    COALESCE((
                        SELECT SUM(b_sub.TotalAmount)
                        FROM dbo.Booking b_sub
                        JOIN paid_bookings pb_sub ON pb_sub.BookingID = b_sub.BookingID
                        JOIN dbo.Showtime st_sub ON st_sub.ShowtimeID = b_sub.ShowtimeID
                        WHERE st_sub.MovieID = m.MovieID
                          AND UPPER(b_sub.[Status]) = 'CONFIRMED'
                          AND CAST(pb_sub.PaidTime AS DATE) BETWEEN ? AND ?
                    ), 0) AS TotalRevenue
                FROM dbo.Movie m
                JOIN dbo.Showtime st ON st.MovieID = m.MovieID
                LEFT JOIN dbo.Booking b ON b.ShowtimeID = st.ShowtimeID
                     AND UPPER(b.[Status]) = 'CONFIRMED'
                LEFT JOIN paid_bookings pb ON pb.BookingID = b.BookingID
                     AND CAST(pb.PaidTime AS DATE) BETWEEN ? AND ?
                LEFT JOIN dbo.BookingSeat bs ON bs.BookingID = b.BookingID
                     AND pb.BookingID IS NOT NULL
                     AND UPPER(bs.[Status]) = 'BOOKED'
                     AND NOT EXISTS (
                         SELECT 1 FROM dbo.Ticket t
                         WHERE t.BookingSeatID = bs.BookingSeatID
                           AND UPPER(t.TicketStatus) = 'CANCELLED'
                     )
                GROUP BY m.MovieID, m.Title
                HAVING COUNT(DISTINCT bs.BookingSeatID) > 0 OR COUNT(DISTINCT CASE WHEN CAST(st.StartTime AS DATE) BETWEEN ? AND ? THEN st.ShowtimeID END) > 0
                ORDER BY TotalRevenue DESC, SoldTickets DESC
                """;

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
           Date.valueOf(startDate), Date.valueOf(endDate),
           Date.valueOf(startDate), Date.valueOf(endDate),
           Date.valueOf(startDate), Date.valueOf(endDate),
           Date.valueOf(startDate), Date.valueOf(endDate));
    }

    /**
     * Query detailed transactions list for reconciliation and export.
     */
    public List<TransactionRevenueItem> getTransactions(LocalDate startDate, LocalDate endDate) {
        String sql = PAID_BOOKINGS_CTE + """
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
                          AND UPPER(bs2.[Status]) = 'BOOKED'
                    ), N'-') AS SeatLabels,
                    COALESCE((
                        SELECT SUM(bs3.UnitPrice)
                        FROM dbo.BookingSeat bs3
                        WHERE bs3.BookingID = b.BookingID
                          AND UPPER(bs3.[Status]) = 'BOOKED'
                          AND NOT EXISTS (
                              SELECT 1 FROM dbo.Ticket t3
                              WHERE t3.BookingSeatID = bs3.BookingSeatID
                                AND UPPER(t3.TicketStatus) = 'CANCELLED'
                          )
                    ), 0) AS TicketAmount,
                    COALESCE((
                        SELECT SUM(bfb.Quantity * bfb.UnitPrice)
                        FROM dbo.BookingFoodBeverage bfb
                        WHERE bfb.BookingID = b.BookingID
                    ), 0) AS FbAmount,
                    COALESCE((
                        SELECT TOP 1 p.Method
                        FROM dbo.Payment p
                        WHERE p.BookingID = b.BookingID
                          AND UPPER(p.[Status]) = 'PAID'
                          AND p.PaidAt IS NOT NULL
                        ORDER BY p.PaidAt DESC, p.PaymentID DESC
                    ), b.BookingType) AS PaymentMethod
                FROM dbo.Booking b
                JOIN paid_bookings pb ON pb.BookingID = b.BookingID
                JOIN dbo.Showtime st ON st.ShowtimeID = b.ShowtimeID
                JOIN dbo.Movie m ON m.MovieID = st.MovieID
                JOIN dbo.Room r ON r.RoomID = st.RoomID
                LEFT JOIN dbo.Customer c ON c.CustomerID = b.CustomerID
                LEFT JOIN dbo.Account a ON a.AccountID = c.AccountID
                WHERE UPPER(b.[Status]) = 'CONFIRMED'
                  AND CAST(pb.PaidTime AS DATE) BETWEEN ? AND ?
                ORDER BY pb.PaidTime DESC, b.BookingID DESC
                """;

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
    }
}

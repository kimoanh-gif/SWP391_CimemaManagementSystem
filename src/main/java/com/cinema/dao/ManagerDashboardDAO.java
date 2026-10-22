package com.cinema.dao;

import com.cinema.model.DashboardBooking;
import com.cinema.model.DashboardShowtime;
import com.cinema.model.DashboardSummary;
import com.cinema.model.RevenueDay;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only queries for the Manager dashboard (M01).
 * Revenue only counts CONFIRMED bookings (paid), grouped by booking date.
 */
@Repository
public class ManagerDashboardDAO {

    private static final String ACTIVE_MOVIE = "UPPER(m.[Status]) NOT IN ('INACTIVE', 'ARCHIVED', 'DELETED')";
    private static final String NOW_SHOWING = "UPPER(m.[Status]) IN ('NOW_SHOWING', 'NOW SHOWING', 'ACTIVE')";
    private static final String COMING_SOON = "UPPER(m.[Status]) IN ('COMING_SOON', 'COMING SOON', 'UPCOMING')";
    private static final String LIVE_SHOWTIME = "UPPER(st.[Status]) NOT IN ('CANCELLED', 'INACTIVE', 'DELETED')";

    private final JdbcTemplate jdbcTemplate;

    public ManagerDashboardDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardSummary loadSummary(LocalDate today) {
        DashboardSummary summary = new DashboardSummary();
        Date todayParam = Date.valueOf(today);
        Date yesterdayParam = Date.valueOf(today.minusDays(1));

        jdbcTemplate.query("""
                SELECT COUNT(*) AS Total,
                       SUM(CASE WHEN %s THEN 1 ELSE 0 END) AS NowShowing,
                       SUM(CASE WHEN %s THEN 1 ELSE 0 END) AS ComingSoon
                FROM dbo.Movie m
                WHERE %s
                """.formatted(NOW_SHOWING, COMING_SOON, ACTIVE_MOVIE), rs -> {
            summary.setTotalMovies(rs.getInt("Total"));
            summary.setNowShowingMovies(rs.getInt("NowShowing"));
            summary.setComingSoonMovies(rs.getInt("ComingSoon"));
        });

        summary.setTotalGenres(count("SELECT COUNT(*) FROM dbo.Genre"));

        jdbcTemplate.query("""
                SELECT COUNT(*) AS Total,
                       SUM(CASE WHEN st.EndTime < SYSDATETIME() THEN 1 ELSE 0 END) AS Finished,
                       SUM(CASE WHEN st.StartTime > SYSDATETIME() THEN 1 ELSE 0 END) AS Upcoming
                FROM dbo.Showtime st
                WHERE CAST(st.StartTime AS date) = ? AND %s
                """.formatted(LIVE_SHOWTIME), rs -> {
            summary.setShowtimesToday(rs.getInt("Total"));
            summary.setShowtimesFinishedToday(rs.getInt("Finished"));
            summary.setShowtimesUpcomingToday(rs.getInt("Upcoming"));
        }, todayParam);

        String ticketSql = """
                SELECT COUNT(*)
                FROM dbo.BookingSeat bs
                JOIN dbo.Booking b ON b.BookingID = bs.BookingID
                WHERE UPPER(b.[Status]) = 'CONFIRMED' AND UPPER(bs.[Status]) = 'BOOKED'
                  AND CAST(b.BookingTime AS date) = ?
                """;
        summary.setTicketsToday(count(ticketSql, todayParam));
        summary.setTicketsYesterday(count(ticketSql, yesterdayParam));

        String revenueSql = """
                SELECT COALESCE(SUM(b.TotalAmount), 0)
                FROM dbo.Booking b
                WHERE UPPER(b.[Status]) = 'CONFIRMED' AND CAST(b.BookingTime AS date) = ?
                """;
        summary.setRevenueToday(jdbcTemplate.queryForObject(revenueSql, BigDecimal.class, todayParam));
        summary.setRevenueYesterday(jdbcTemplate.queryForObject(revenueSql, BigDecimal.class, yesterdayParam));

        summary.setActivePromotions(count("""
                SELECT COUNT(*) FROM dbo.Promotion
                WHERE UPPER([Status]) = 'ACTIVE' AND StartDate <= SYSDATETIME() AND EndDate >= SYSDATETIME()
                """));
        summary.setActiveVouchers(count("""
                SELECT COUNT(*) FROM dbo.Voucher
                WHERE UPPER([Status]) = 'ACTIVE' AND ValidFrom <= SYSDATETIME() AND ValidTo >= SYSDATETIME()
                """));
        return summary;
    }

    /** Ticket + F&B revenue per day between {@code from} and {@code to} (inclusive). Missing days are absent. */
    public Map<LocalDate, RevenueDay> findRevenueByDay(LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> tickets = new HashMap<>();
        jdbcTemplate.query("""
                SELECT CAST(b.BookingTime AS date) AS Day, SUM(bs.UnitPrice) AS Amount
                FROM dbo.Booking b
                JOIN dbo.BookingSeat bs ON bs.BookingID = b.BookingID AND UPPER(bs.[Status]) = 'BOOKED'
                WHERE UPPER(b.[Status]) = 'CONFIRMED'
                  AND CAST(b.BookingTime AS date) BETWEEN ? AND ?
                GROUP BY CAST(b.BookingTime AS date)
                """, rs -> {
            tickets.put(rs.getDate("Day").toLocalDate(), rs.getBigDecimal("Amount"));
        }, Date.valueOf(from), Date.valueOf(to));

        Map<LocalDate, BigDecimal> fnb = new HashMap<>();
        jdbcTemplate.query("""
                SELECT CAST(b.BookingTime AS date) AS Day,
                       SUM(COALESCE(f.Subtotal, f.Quantity * f.UnitPrice)) AS Amount
                FROM dbo.Booking b
                JOIN dbo.BookingFoodBeverage f ON f.BookingID = b.BookingID
                WHERE UPPER(b.[Status]) = 'CONFIRMED'
                  AND CAST(b.BookingTime AS date) BETWEEN ? AND ?
                GROUP BY CAST(b.BookingTime AS date)
                """, rs -> {
            fnb.put(rs.getDate("Day").toLocalDate(), rs.getBigDecimal("Amount"));
        }, Date.valueOf(from), Date.valueOf(to));

        Map<LocalDate, RevenueDay> result = new HashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (tickets.containsKey(day) || fnb.containsKey(day)) {
                result.put(day, new RevenueDay(day, tickets.get(day), fnb.get(day)));
            }
        }
        return result;
    }

    /** Ticket + F&B revenue of today grouped by booking hour (0-23). */
    public Map<Integer, RevenueDay> findRevenueByHour(LocalDate day) {
        Map<Integer, BigDecimal> tickets = new HashMap<>();
        jdbcTemplate.query("""
                SELECT DATEPART(HOUR, b.BookingTime) AS Hr, SUM(bs.UnitPrice) AS Amount
                FROM dbo.Booking b
                JOIN dbo.BookingSeat bs ON bs.BookingID = b.BookingID AND UPPER(bs.[Status]) = 'BOOKED'
                WHERE UPPER(b.[Status]) = 'CONFIRMED' AND CAST(b.BookingTime AS date) = ?
                GROUP BY DATEPART(HOUR, b.BookingTime)
                """, rs -> {
            tickets.put(rs.getInt("Hr"), rs.getBigDecimal("Amount"));
        }, Date.valueOf(day));

        Map<Integer, BigDecimal> fnb = new HashMap<>();
        jdbcTemplate.query("""
                SELECT DATEPART(HOUR, b.BookingTime) AS Hr,
                       SUM(COALESCE(f.Subtotal, f.Quantity * f.UnitPrice)) AS Amount
                FROM dbo.Booking b
                JOIN dbo.BookingFoodBeverage f ON f.BookingID = b.BookingID
                WHERE UPPER(b.[Status]) = 'CONFIRMED' AND CAST(b.BookingTime AS date) = ?
                GROUP BY DATEPART(HOUR, b.BookingTime)
                """, rs -> {
            fnb.put(rs.getInt("Hr"), rs.getBigDecimal("Amount"));
        }, Date.valueOf(day));

        Map<Integer, RevenueDay> result = new HashMap<>();
        for (int hour = 0; hour < 24; hour++) {
            if (tickets.containsKey(hour) || fnb.containsKey(hour)) {
                result.put(hour, new RevenueDay(day, tickets.get(hour), fnb.get(hour)));
            }
        }
        return result;
    }

    public List<DashboardShowtime> findUpcomingShowtimes(int limit) {
        String sql = """
                SELECT TOP (%d) st.ShowtimeID, st.StartTime, m.Title, m.AgeRating, r.RoomName,
                       (SELECT COUNT(*) FROM dbo.BookingSeat bs
                         WHERE bs.ShowtimeID = st.ShowtimeID AND UPPER(bs.[Status]) IN ('BOOKED', 'HELD')) AS Booked,
                       COALESCE(NULLIF((SELECT COUNT(*) FROM dbo.Seat s
                                         WHERE s.RoomID = r.RoomID AND UPPER(s.[Status]) = 'ACTIVE'), 0),
                                r.Capacity) AS Capacity
                FROM dbo.Showtime st
                JOIN dbo.Movie m ON m.MovieID = st.MovieID
                JOIN dbo.Room r ON r.RoomID = st.RoomID
                WHERE st.StartTime >= SYSDATETIME() AND %s
                ORDER BY st.StartTime
                """.formatted(Math.max(1, limit), LIVE_SHOWTIME);
        return jdbcTemplate.query(sql, (rs, i) -> {
            DashboardShowtime row = new DashboardShowtime();
            row.setShowtimeId(rs.getInt("ShowtimeID"));
            Timestamp start = rs.getTimestamp("StartTime");
            row.setStartTime(start == null ? null : start.toLocalDateTime());
            row.setMovieTitle(rs.getString("Title"));
            row.setAgeRating(rs.getString("AgeRating"));
            row.setRoomName(rs.getString("RoomName"));
            row.setBookedSeats(rs.getInt("Booked"));
            row.setCapacity(rs.getInt("Capacity"));
            return row;
        });
    }

    public List<DashboardBooking> findRecentBookings(int limit) {
        String sql = """
                SELECT TOP (%d) b.BookingCode, b.BookingType, b.[Status], b.TotalAmount,
                       acc.FullName, acc.Phone, m.Title, r.RoomName, st.StartTime,
                       CASE WHEN EXISTS (SELECT 1 FROM dbo.Ticket t
                                         JOIN dbo.BookingSeat bs ON bs.BookingSeatID = t.BookingSeatID
                                         WHERE bs.BookingID = b.BookingID AND UPPER(t.TicketStatus) = 'USED')
                            THEN 1 ELSE 0 END AS CheckedIn
                FROM dbo.Booking b
                JOIN dbo.Showtime st ON st.ShowtimeID = b.ShowtimeID
                JOIN dbo.Movie m ON m.MovieID = st.MovieID
                JOIN dbo.Room r ON r.RoomID = st.RoomID
                LEFT JOIN dbo.Customer c ON c.CustomerID = b.CustomerID
                LEFT JOIN dbo.Account acc ON acc.AccountID = c.AccountID
                ORDER BY b.BookingTime DESC
                """.formatted(Math.max(1, limit));
        return jdbcTemplate.query(sql, (rs, i) -> {
            DashboardBooking row = new DashboardBooking();
            row.setBookingCode(rs.getString("BookingCode"));
            row.setBookingType(rs.getString("BookingType"));
            row.setStatus(rs.getString("Status"));
            row.setTotalAmount(rs.getBigDecimal("TotalAmount"));
            row.setCustomerName(rs.getString("FullName"));
            row.setCustomerPhone(rs.getString("Phone"));
            row.setMovieTitle(rs.getString("Title"));
            row.setRoomName(rs.getString("RoomName"));
            Timestamp start = rs.getTimestamp("StartTime");
            row.setShowtimeStart(start == null ? null : start.toLocalDateTime());
            row.setCheckedIn(rs.getInt("CheckedIn") == 1);
            return row;
        });
    }

    /** [activeRooms, totalRooms] for the header pill. */
    public int[] countRooms() {
        int[] result = new int[2];
        jdbcTemplate.query("""
                SELECT SUM(CASE WHEN UPPER([Status]) = 'ACTIVE' THEN 1 ELSE 0 END) AS Active, COUNT(*) AS Total
                FROM dbo.Room
                """, rs -> {
            result[0] = rs.getInt("Active");
            result[1] = rs.getInt("Total");
        });
        return result;
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }
}

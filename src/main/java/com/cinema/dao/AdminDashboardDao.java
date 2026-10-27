package com.cinema.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Read-only dashboard queries. Each paid booking is counted once, even after retries. */
@Repository
public class AdminDashboardDao {
    private final JdbcTemplate jdbc;
    public AdminDashboardDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // doanh thu theo 
    private static final String PAID = """
            WITH paid AS (
                SELECT BookingID, MAX(Amount) Amount, MIN(COALESCE(PaidAt, CreatedAt)) PaidTime
                FROM dbo.Payment WHERE UPPER([Status]) IN ('PAID', 'SUCCESS', 'COMPLETED')
                GROUP BY BookingID
            )
            """;

    public Map<String, Object> summary(LocalDateTime start, LocalDateTime end) {
        return jdbc.queryForMap(PAID + """
            SELECT
              (SELECT COALESCE(SUM(Amount),0) FROM paid WHERE PaidTime >= ? AND PaidTime < ?) revenue,
              (SELECT COUNT(*) FROM dbo.BookingSeat bs JOIN paid p ON p.BookingID=bs.BookingID
               WHERE p.PaidTime >= ? AND p.PaidTime < ? AND UPPER(bs.[Status]) IN ('BOOKED','CONFIRMED')) tickets,
              (SELECT COUNT(*) FROM dbo.Booking WHERE UPPER([Status]) IN ('PENDING','PAYMENT_PENDING')) pending,
              (SELECT COUNT(*) FROM dbo.Customer c JOIN dbo.Account a ON a.AccountID=c.AccountID
               WHERE UPPER(a.[Status])='ACTIVE') activeCustomers,
              (SELECT COUNT(*) FROM dbo.Customer c JOIN dbo.Account a ON a.AccountID=c.AccountID
               WHERE a.CreatedAt >= ? AND a.CreatedAt < ?) newCustomers,
              (SELECT COUNT(*) FROM dbo.Customer) customers,
              (SELECT COUNT(*) FROM dbo.Account a JOIN dbo.[Role] r ON r.RoleID=a.RoleID
               WHERE UPPER(r.Name) IN ('STAFF','MANAGER')) staff,
              (SELECT COUNT(*) FROM dbo.[Role]) roles,
              (SELECT COUNT(*) FROM dbo.Account WHERE UPPER([Status]) IN ('LOCKED','SUSPENDED','INACTIVE')) locked
            """, ts(start), ts(end), ts(start), ts(end), ts(start), ts(end));
    }

    public java.math.BigDecimal revenue(LocalDateTime start, LocalDateTime end) {
        return jdbc.queryForObject(PAID + """
            SELECT COALESCE(SUM(Amount), 0) FROM paid
            WHERE PaidTime >= ? AND PaidTime < ?
            """, java.math.BigDecimal.class, ts(start), ts(end));
    }

    public List<Map<String, Object>> daily(LocalDateTime start, LocalDateTime end) {
        return jdbc.queryForList(PAID + """
            SELECT CAST(PaidTime AS date) day, SUM(Amount) amount FROM paid
            WHERE PaidTime >= ? AND PaidTime < ? GROUP BY CAST(PaidTime AS date) ORDER BY day
            """, ts(start), ts(end));
    }

// top 5 phim có doanh thu cao nhất
    public List<Map<String, Object>> topMovies(LocalDateTime start, LocalDateTime end) {
        return jdbc.queryForList(PAID + """
            SELECT TOP (5) m.Title title, SUM(p.Amount) amount, COUNT(*) bookings
            FROM paid p JOIN dbo.Booking b ON b.BookingID=p.BookingID
            JOIN dbo.Showtime st ON st.ShowtimeID=b.ShowtimeID JOIN dbo.Movie m ON m.MovieID=st.MovieID
            WHERE p.PaidTime >= ? AND p.PaidTime < ?
            GROUP BY m.MovieID,m.Title ORDER BY SUM(p.Amount) DESC,m.MovieID
            """, ts(start), ts(end));
    }

    public List<Map<String, Object>> recentBookings() {
        return jdbc.queryForList("""
            SELECT TOP (6) b.BookingCode code,m.Title title,st.StartTime startTime,
                b.BookingTime createdAt,b.TotalAmount amount,b.[Status] status,b.BookingType channel
            FROM dbo.Booking b JOIN dbo.Showtime st ON st.ShowtimeID=b.ShowtimeID
            JOIN dbo.Movie m ON m.MovieID=st.MovieID ORDER BY b.BookingTime DESC,b.BookingID DESC
            """);
    }

    public List<Map<String, Object>> upcoming(LocalDateTime now, LocalDateTime end) {
        return jdbc.queryForList("""
            SELECT TOP (4) m.Title title,r.RoomName room,st.StartTime startTime,
              (SELECT COUNT(*) FROM dbo.Seat s WHERE s.RoomID=st.RoomID AND UPPER(s.[Status])='ACTIVE') capacity,
              (SELECT COUNT(*) FROM dbo.BookingSeat bs JOIN dbo.Booking b ON b.BookingID=bs.BookingID
               WHERE bs.ShowtimeID=st.ShowtimeID AND UPPER(b.[Status]) NOT IN ('CANCELLED','EXPIRED')
               AND (UPPER(bs.[Status]) IN ('BOOKED','CONFIRMED')
                    OR (UPPER(bs.[Status])='HELD' AND bs.HeldUntil > ?))) reserved
            FROM dbo.Showtime st JOIN dbo.Movie m ON m.MovieID=st.MovieID
            JOIN dbo.Room r ON r.RoomID=st.RoomID
            WHERE st.StartTime >= ? AND st.StartTime < ?
              AND UPPER(st.[Status]) IN ('SCHEDULED','ACTIVE') AND UPPER(r.[Status])='ACTIVE'
            ORDER BY st.StartTime,st.ShowtimeID
            """, ts(now), ts(now), ts(end));
    }
    private Timestamp ts(LocalDateTime time) { return Timestamp.valueOf(time); }
}

package com.cinema.dao;

import com.cinema.model.PublicShowtime;
import com.cinema.model.StaffBookingDetail;
import com.cinema.model.StaffBookingListItem;
import com.cinema.model.StaffDashboardData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class StaffOperationsDAO {

    private final JdbcTemplate jdbcTemplate;

    public StaffOperationsDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public StaffDashboardData getDashboardData(Integer staffId, boolean allStaff) {
        Integer sold = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM dbo.BookingSeat bs
                JOIN dbo.Booking b ON b.BookingID = bs.BookingID
                WHERE UPPER(b.BookingType) = 'COUNTER'
                  AND UPPER(b.[Status]) = 'CONFIRMED'
                  AND CAST(b.BookingTime AS date) = CAST(SYSDATETIME() AS date)
                  AND (? = 1 OR b.StaffID = ?)
                """, Integer.class, allStaff ? 1 : 0, staffId);
        Integer pending = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM dbo.Booking b
                WHERE UPPER(b.BookingType) = 'COUNTER'
                  AND UPPER(b.[Status]) = 'PENDING'
                  AND (? = 1 OR b.StaffID = ?)
                """, Integer.class, allStaff ? 1 : 0, staffId);
        List<PublicShowtime> upcoming = jdbcTemplate.query("""
                SELECT TOP (10) st.ShowtimeID, st.MovieID, st.RoomID, st.StartTime, st.EndTime,
                       st.BasePrice, st.[Status], m.Title, m.PosterURL, m.AgeRating, m.Duration,
                       r.RoomName, r.RoomType, r.Capacity AS TotalSeats,
                       (SELECT COUNT(*) FROM dbo.Seat seat
                        WHERE seat.RoomID = st.RoomID AND UPPER(seat.[Status]) = 'ACTIVE'
                          AND NOT EXISTS (
                              SELECT 1 FROM dbo.BookingSeat bs
                              JOIN dbo.Booking b ON b.BookingID = bs.BookingID
                              WHERE bs.ShowtimeID = st.ShowtimeID AND bs.SeatID = seat.SeatID
                                AND UPPER(b.[Status]) NOT IN ('CANCELLED', 'EXPIRED')
                                AND (UPPER(bs.[Status]) IN ('BOOKED', 'CONFIRMED')
                                     OR (UPPER(bs.[Status]) = 'HELD' AND bs.HeldUntil > SYSDATETIME()))
                          )) AS AvailableSeats,
                       (SELECT STRING_AGG(g.[Name], ', ')
                        FROM dbo.MovieGenre mg JOIN dbo.Genre g ON g.GenreID = mg.GenreID
                        WHERE mg.MovieID = m.MovieID) AS Genres
                FROM dbo.Showtime st
                JOIN dbo.Movie m ON m.MovieID = st.MovieID
                JOIN dbo.Room r ON r.RoomID = st.RoomID
                WHERE st.StartTime BETWEEN SYSDATETIME() AND DATEADD(HOUR, 2, SYSDATETIME())
                  AND UPPER(st.[Status]) IN ('SCHEDULED', 'ACTIVE')
                  AND UPPER(r.[Status]) = 'ACTIVE'
                ORDER BY st.StartTime
                """, (rs, rowNum) -> {
            PublicShowtime showtime = new PublicShowtime();
            showtime.setShowtimeId(rs.getInt("ShowtimeID"));
            showtime.setMovieId(rs.getInt("MovieID"));
            showtime.setRoomId(rs.getInt("RoomID"));
            showtime.setMovieTitle(rs.getString("Title"));
            showtime.setMoviePosterUrl(rs.getString("PosterURL"));
            showtime.setMovieAgeRating(rs.getString("AgeRating"));
            showtime.setMovieDuration(rs.getInt("Duration"));
            showtime.setMovieGenres(rs.getString("Genres"));
            showtime.setRoomName(rs.getString("RoomName"));
            showtime.setRoomType(rs.getString("RoomType"));
            showtime.setStartTime(rs.getObject("StartTime", java.time.LocalDateTime.class));
            showtime.setEndTime(rs.getObject("EndTime", java.time.LocalDateTime.class));
            showtime.setBasePrice(rs.getBigDecimal("BasePrice"));
            showtime.setStatus(rs.getString("Status"));
            showtime.setAvailableSeats(rs.getInt("AvailableSeats"));
            showtime.setTotalSeats(rs.getInt("TotalSeats"));
            return showtime;
        });
        return new StaffDashboardData(sold == null ? 0 : sold, pending == null ? 0 : pending, upcoming);
    }

    public long countBookings(Integer staffId, boolean allStaff, String query, String status) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT_BIG(*)
                FROM dbo.Booking b
                LEFT JOIN dbo.Customer c ON c.CustomerID = b.CustomerID
                LEFT JOIN dbo.Account a ON a.AccountID = c.AccountID
                WHERE (? = 1 OR b.StaffID = ?)
                  AND (? IS NULL OR b.BookingCode LIKE ? OR COALESCE(a.Phone, '') LIKE ?
                       OR COALESCE(a.FullName, '') LIKE ?)
                  AND (? IS NULL OR UPPER(b.[Status]) = UPPER(?))
                """, Long.class, allStaff ? 1 : 0, staffId, query,
                query == null ? null : "%" + query + "%",
                query == null ? null : "%" + query + "%",
                query == null ? null : "%" + query + "%", status, status);
        return count == null ? 0 : count;
    }

    public List<StaffBookingListItem> findBookings(Integer staffId, boolean allStaff,
                                                    String query, String status, int offset, int limit) {
        return jdbcTemplate.query("""
                SELECT b.BookingID, b.BookingCode, a.FullName, a.Phone, m.Title,
                       st.StartTime, b.BookingType, b.[Status], b.TotalAmount
                FROM dbo.Booking b
                LEFT JOIN dbo.Customer c ON c.CustomerID = b.CustomerID
                LEFT JOIN dbo.Account a ON a.AccountID = c.AccountID
                JOIN dbo.Showtime st ON st.ShowtimeID = b.ShowtimeID
                JOIN dbo.Movie m ON m.MovieID = st.MovieID
                WHERE (? = 1 OR b.StaffID = ?)
                  AND (? IS NULL OR b.BookingCode LIKE ? OR COALESCE(a.Phone, '') LIKE ?
                       OR COALESCE(a.FullName, '') LIKE ?)
                  AND (? IS NULL OR UPPER(b.[Status]) = UPPER(?))
                ORDER BY b.BookingTime DESC, b.BookingID DESC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, (rs, rowNum) -> new StaffBookingListItem(
                rs.getInt("BookingID"), rs.getString("BookingCode"),
                rs.getString("FullName"), rs.getString("Phone"), rs.getString("Title"),
                rs.getObject("StartTime", java.time.LocalDateTime.class),
                rs.getString("BookingType"), rs.getString("Status"), rs.getBigDecimal("TotalAmount")
        ), allStaff ? 1 : 0, staffId, query,
                query == null ? null : "%" + query + "%",
                query == null ? null : "%" + query + "%",
                query == null ? null : "%" + query + "%", status, status, offset, limit);
    }

    public Optional<StaffBookingDetail> findBookingDetail(int bookingId, Integer staffId, boolean allStaff) {
        List<StaffBookingDetail> matches = jdbcTemplate.query("""
                SELECT b.BookingID, b.BookingCode, b.BookingType, b.[Status], b.BookingTime,
                       a.FullName, a.Phone, a.Email, m.Title, r.RoomName, st.StartTime,
                       b.Subtotal, b.DiscountAmount, b.TotalAmount
                FROM dbo.Booking b
                LEFT JOIN dbo.Customer c ON c.CustomerID = b.CustomerID
                LEFT JOIN dbo.Account a ON a.AccountID = c.AccountID
                JOIN dbo.Showtime st ON st.ShowtimeID = b.ShowtimeID
                JOIN dbo.Movie m ON m.MovieID = st.MovieID
                JOIN dbo.Room r ON r.RoomID = st.RoomID
                WHERE b.BookingID = ? AND (? = 1 OR b.StaffID = ?)
                """, (rs, rowNum) -> new StaffBookingDetail(
                rs.getInt("BookingID"), rs.getString("BookingCode"), rs.getString("BookingType"),
                rs.getString("Status"), rs.getObject("BookingTime", java.time.LocalDateTime.class),
                rs.getString("FullName"), rs.getString("Phone"), rs.getString("Email"),
                rs.getString("Title"), rs.getString("RoomName"),
                rs.getObject("StartTime", java.time.LocalDateTime.class),
                rs.getBigDecimal("Subtotal"), rs.getBigDecimal("DiscountAmount"),
                rs.getBigDecimal("TotalAmount"), List.of(), List.of()
        ), bookingId, allStaff ? 1 : 0, staffId);
        if (matches.isEmpty()) return Optional.empty();
        StaffBookingDetail header = matches.get(0);
        List<StaffBookingDetail.SeatLine> seats = jdbcTemplate.query("""
                SELECT s.RowLabel, s.SeatNumber, st.TypeName, bs.UnitPrice, bs.[Status]
                FROM dbo.BookingSeat bs
                JOIN dbo.Seat s ON s.SeatID = bs.SeatID
                JOIN dbo.SeatType st ON st.SeatTypeID = s.SeatTypeID
                WHERE bs.BookingID = ?
                ORDER BY s.GridRow, s.GridColumn
                """, (rs, rowNum) -> new StaffBookingDetail.SeatLine(
                rs.getString("RowLabel") + rs.getInt("SeatNumber"),
                rs.getString("TypeName"), rs.getBigDecimal("UnitPrice"), rs.getString("Status")
        ), bookingId);
        List<StaffBookingDetail.FoodLine> foods = jdbcTemplate.query("""
                SELECT item.[Name], bfb.Quantity, bfb.UnitPrice,
                       bfb.Quantity * bfb.UnitPrice AS Subtotal
                FROM dbo.BookingFoodBeverage bfb
                JOIN dbo.FoodBeverageItem item ON item.FoodBeverageItemID = bfb.FoodBeverageItemID
                WHERE bfb.BookingID = ?
                ORDER BY item.[Name]
                """, (rs, rowNum) -> new StaffBookingDetail.FoodLine(
                rs.getString("Name"), rs.getInt("Quantity"),
                rs.getBigDecimal("UnitPrice"), rs.getBigDecimal("Subtotal")
        ), bookingId);
        return Optional.of(new StaffBookingDetail(
                header.bookingId(), header.bookingCode(), header.bookingType(), header.status(),
                header.bookingTime(), header.customerName(), header.phone(), header.email(),
                header.movieTitle(), header.roomName(), header.showtime(), header.subtotal(),
                header.discountAmount(), header.totalAmount(), seats, foods
        ));
    }

    public int checkInBooking(String bookingCode) {
        return jdbcTemplate.update("""
                UPDATE t
                SET TicketStatus = 'USED', CheckedInAt = SYSDATETIME()
                FROM dbo.Ticket t
                JOIN dbo.BookingSeat bs ON bs.BookingSeatID = t.BookingSeatID
                JOIN dbo.Booking b ON b.BookingID = bs.BookingID
                JOIN dbo.Showtime st ON st.ShowtimeID = b.ShowtimeID
                WHERE b.BookingCode = ?
                  AND UPPER(b.[Status]) = 'CONFIRMED'
                  AND st.StartTime <= DATEADD(MINUTE, 30, SYSDATETIME())
                  AND st.EndTime >= SYSDATETIME()
                  AND UPPER(t.TicketStatus) = 'VALID'
                """, bookingCode.trim());
    }

    public int countCheckInTickets(String bookingCode) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM dbo.Ticket t
                JOIN dbo.BookingSeat bs ON bs.BookingSeatID = t.BookingSeatID
                JOIN dbo.Booking b ON b.BookingID = bs.BookingID
                WHERE b.BookingCode = ? AND UPPER(t.TicketStatus) = 'USED'
                """, Integer.class, bookingCode.trim());
        return count == null ? 0 : count;
    }

    public boolean hasBooking(String bookingCode) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM dbo.Booking WHERE BookingCode = ?
                """, Integer.class, bookingCode.trim());
        return count != null && count > 0;
    }
}

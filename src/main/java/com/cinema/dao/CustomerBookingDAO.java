package com.cinema.dao;

import com.cinema.model.BookingSummary;
import com.cinema.model.FoodBeverageOption;
import com.cinema.model.MovieBookingOption;
import com.cinema.model.SeatOption;
import com.cinema.model.ShowtimeOption;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class CustomerBookingDAO {

    private final JdbcTemplate jdbcTemplate;

    public CustomerBookingDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MovieBookingOption> findMoviesWithShowtimes(LocalDate date) {
        String sql = """
                SELECT m.MovieID, m.Title, m.Duration, m.ReleaseDate, m.[Language],
                       m.Director, m.AgeRating, m.[Description], m.PosterURL,
                       (SELECT STRING_AGG(g.[Name], ', ')
                        FROM dbo.MovieGenre mg
                        JOIN dbo.Genre g ON g.GenreID = mg.GenreID
                        WHERE mg.MovieID = m.MovieID) AS Genres
                FROM dbo.Movie m
                WHERE UPPER(m.[Status]) NOT IN ('INACTIVE', 'DISABLED', 'ARCHIVED')
                  AND EXISTS (
                      SELECT 1 FROM dbo.Showtime st
                      WHERE st.MovieID = m.MovieID
                        AND CAST(st.StartTime AS DATE) = ?
                        AND st.StartTime > SYSDATETIME()
                        AND UPPER(st.[Status]) IN ('SCHEDULED', 'ACTIVE')
                  )
                ORDER BY m.Title
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            MovieBookingOption movie = new MovieBookingOption();
            movie.setMovieId(rs.getInt("MovieID"));
            movie.setTitle(rs.getString("Title"));
            movie.setDuration(rs.getInt("Duration"));
            Date releaseDate = rs.getDate("ReleaseDate");
            movie.setReleaseDate(releaseDate == null ? null : releaseDate.toLocalDate());
            movie.setLanguage(rs.getString("Language"));
            movie.setDirector(rs.getString("Director"));
            movie.setAgeRating(rs.getString("AgeRating"));
            movie.setDescription(rs.getString("Description"));
            movie.setPosterUrl(rs.getString("PosterURL"));
            movie.setGenres(rs.getString("Genres"));
            return movie;
        }, Date.valueOf(date));
    }

    public List<ShowtimeOption> findShowtimes(int movieId, LocalDate date) {
        String sql = """
                SELECT st.ShowtimeID, st.MovieID, st.RoomID, st.StartTime, st.EndTime,
                       st.BasePrice, m.Title, m.PosterURL, m.AgeRating, m.Duration,
                       r.RoomName, r.RoomType,
                       (SELECT COUNT(*)
                        FROM dbo.Seat seat
                        WHERE seat.RoomID = st.RoomID
                          AND UPPER(seat.[Status]) = 'ACTIVE'
                          AND NOT EXISTS (
                              SELECT 1
                              FROM dbo.BookingSeat bs
                              JOIN dbo.Booking b ON b.BookingID = bs.BookingID
                              WHERE bs.ShowtimeID = st.ShowtimeID
                                AND bs.SeatID = seat.SeatID
                                AND UPPER(b.[Status]) NOT IN ('CANCELLED', 'EXPIRED')
                                AND (UPPER(bs.[Status]) IN ('BOOKED', 'CONFIRMED')
                                     OR (UPPER(bs.[Status]) = 'HELD' AND bs.HeldUntil > SYSDATETIME()))
                          )) AS AvailableSeats
                FROM dbo.Showtime st
                JOIN dbo.Movie m ON m.MovieID = st.MovieID
                JOIN dbo.Room r ON r.RoomID = st.RoomID
                WHERE st.MovieID = ?
                  AND CAST(st.StartTime AS DATE) = ?
                  AND st.StartTime > SYSDATETIME()
                  AND UPPER(st.[Status]) IN ('SCHEDULED', 'ACTIVE')
                  AND UPPER(r.[Status]) IN ('ACTIVE', 'READY', 'SCREENING')
                ORDER BY st.StartTime
                """;
        return jdbcTemplate.query(sql, this::mapShowtime, movieId, Date.valueOf(date));
    }

    public Optional<ShowtimeOption> findShowtime(int showtimeId) {
        String sql = """
                SELECT st.ShowtimeID, st.MovieID, st.RoomID, st.StartTime, st.EndTime,
                       st.BasePrice, m.Title, m.PosterURL, m.AgeRating, m.Duration,
                       r.RoomName, r.RoomType, 0 AS AvailableSeats
                FROM dbo.Showtime st
                JOIN dbo.Movie m ON m.MovieID = st.MovieID
                JOIN dbo.Room r ON r.RoomID = st.RoomID
                WHERE st.ShowtimeID = ?
                  AND st.StartTime > SYSDATETIME()
                  AND UPPER(st.[Status]) IN ('SCHEDULED', 'ACTIVE')
                  AND UPPER(r.[Status]) IN ('ACTIVE', 'READY', 'SCREENING')
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, this::mapShowtime, showtimeId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public List<SeatOption> findSeats(int showtimeId) {
        String sql = """
                SELECT seat.SeatID, seat.RowLabel, seat.SeatNumber, seat.GridRow, seat.GridColumn,
                       seatType.TypeName, seatType.PriceMultiplier,
                       CAST(st.BasePrice * seatType.PriceMultiplier AS DECIMAL(18,2)) AS Price,
                       CASE WHEN EXISTS (
                           SELECT 1
                           FROM dbo.BookingSeat bs
                           JOIN dbo.Booking b ON b.BookingID = bs.BookingID
                           WHERE bs.ShowtimeID = st.ShowtimeID
                             AND bs.SeatID = seat.SeatID
                             AND UPPER(b.[Status]) NOT IN ('CANCELLED', 'EXPIRED')
                             AND (UPPER(bs.[Status]) IN ('BOOKED', 'CONFIRMED')
                                  OR (UPPER(bs.[Status]) = 'HELD' AND bs.HeldUntil > SYSDATETIME()))
                       ) THEN 0 ELSE 1 END AS Available
                FROM dbo.Showtime st
                JOIN dbo.Seat seat ON seat.RoomID = st.RoomID
                JOIN dbo.SeatType seatType ON seatType.SeatTypeID = seat.SeatTypeID
                WHERE st.ShowtimeID = ?
                  AND UPPER(seat.[Status]) = 'ACTIVE'
                  AND UPPER(seatType.[Status]) = 'ACTIVE'
                ORDER BY seat.GridRow, seat.GridColumn
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            SeatOption seat = new SeatOption();
            seat.setSeatId(rs.getInt("SeatID"));
            seat.setRowLabel(rs.getString("RowLabel"));
            seat.setSeatNumber(rs.getInt("SeatNumber"));
            seat.setGridRow(rs.getInt("GridRow"));
            seat.setGridColumn(rs.getInt("GridColumn"));
            seat.setTypeName(rs.getString("TypeName"));
            seat.setPriceMultiplier(rs.getBigDecimal("PriceMultiplier"));
            seat.setPrice(rs.getBigDecimal("Price"));
            seat.setAvailable(rs.getBoolean("Available"));
            return seat;
        }, showtimeId);
    }

    public Optional<Integer> findCustomerId(int accountId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT CustomerID FROM dbo.Customer WHERE AccountID = ?",
                    Integer.class,
                    accountId
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public int createPendingBooking(int customerId, int showtimeId, String bookingCode) {
        Integer id = jdbcTemplate.queryForObject("""
                INSERT INTO dbo.Booking
                    (CustomerID, ShowtimeID, BookingCode, BookingType, Subtotal,
                     DiscountAmount, TotalAmount, [Status])
                OUTPUT inserted.BookingID
                VALUES (?, ?, ?, 'ONLINE', 0, 0, 0, 'PENDING')
                """, Integer.class, customerId, showtimeId, bookingCode);
        if (id == null) throw new IllegalStateException("Could not create booking.");
        return id;
    }

    public int createPendingCounterBooking(Integer customerId, int staffId, int showtimeId,
                                           String bookingCode) {
        Integer id = jdbcTemplate.queryForObject("""
                INSERT INTO dbo.Booking
                    (CustomerID, StaffID, ShowtimeID, BookingCode, BookingType, Subtotal,
                     DiscountAmount, TotalAmount, [Status])
                OUTPUT inserted.BookingID
                VALUES (?, ?, ?, ?, 'COUNTER', 0, 0, 0, 'PENDING')
                """, Integer.class, customerId, staffId, showtimeId, bookingCode);
        if (id == null) throw new IllegalStateException("Could not create counter booking.");
        return id;
    }

    public void holdSeat(int bookingId, int showtimeId, SeatOption seat, LocalDateTime heldUntil) {
        jdbcTemplate.update("""
                INSERT INTO dbo.BookingSeat
                    (BookingID, SeatID, ShowtimeID, UnitPrice, [Status], HeldUntil)
                VALUES (?, ?, ?, ?, 'HELD', ?)
                """, bookingId, seat.getSeatId(), showtimeId, seat.getPrice(), heldUntil);
    }

    public void recalculateBookingTotal(int bookingId) {
        jdbcTemplate.update("""
                UPDATE b
                SET Subtotal = totals.Subtotal,
                    TotalAmount = totals.Subtotal - b.DiscountAmount
                FROM dbo.Booking b
                CROSS APPLY (
                    SELECT
                        COALESCE((SELECT SUM(bs.UnitPrice)
                                  FROM dbo.BookingSeat bs
                                  WHERE bs.BookingID = b.BookingID), 0)
                      + COALESCE((SELECT SUM(bfb.Quantity * bfb.UnitPrice)
                                  FROM dbo.BookingFoodBeverage bfb
                                  WHERE bfb.BookingID = b.BookingID), 0) AS Subtotal
                ) totals
                WHERE b.BookingID = ?
                """, bookingId);
    }

    public Optional<BookingSummary> findBookingSummary(int bookingId, int accountId) {
        String sql = """
                SELECT b.BookingID, b.BookingCode, b.TotalAmount, b.[Status],
                       m.Title, r.RoomName, st.StartTime,
                       seats.SeatLabels, seats.SeatCount, seats.SeatTotal,
                       COALESCE(food.FoodTotal, 0) AS FoodTotal,
                       seats.HeldUntil
                FROM dbo.Booking b
                JOIN dbo.Customer c ON c.CustomerID = b.CustomerID
                JOIN dbo.Showtime st ON st.ShowtimeID = b.ShowtimeID
                JOIN dbo.Movie m ON m.MovieID = st.MovieID
                JOIN dbo.Room r ON r.RoomID = st.RoomID
                CROSS APPLY (
                    SELECT STRING_AGG(CONCAT(s.RowLabel, s.SeatNumber), ', ') AS SeatLabels,
                           COUNT(*) AS SeatCount,
                           COALESCE(SUM(bs.UnitPrice), 0) AS SeatTotal,
                           MIN(bs.HeldUntil) AS HeldUntil
                    FROM dbo.BookingSeat bs
                    JOIN dbo.Seat s ON s.SeatID = bs.SeatID
                    WHERE bs.BookingID = b.BookingID
                ) seats
                OUTER APPLY (
                    SELECT SUM(bfb.Quantity * bfb.UnitPrice) AS FoodTotal
                    FROM dbo.BookingFoodBeverage bfb
                    WHERE bfb.BookingID = b.BookingID
                ) food
                WHERE b.BookingID = ? AND c.AccountID = ?
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                BookingSummary summary = new BookingSummary();
                summary.setBookingId(rs.getInt("BookingID"));
                summary.setBookingCode(rs.getString("BookingCode"));
                summary.setMovieTitle(rs.getString("Title"));
                summary.setRoomName(rs.getString("RoomName"));
                summary.setStartTime(rs.getObject("StartTime", LocalDateTime.class));
                summary.setSeatLabels(rs.getString("SeatLabels"));
                summary.setSeatCount(rs.getInt("SeatCount"));
                summary.setSeatTotal(rs.getBigDecimal("SeatTotal"));
                summary.setFoodTotal(rs.getBigDecimal("FoodTotal"));
                summary.setTotalAmount(rs.getBigDecimal("TotalAmount"));
                summary.setHeldUntil(rs.getObject("HeldUntil", LocalDateTime.class));
                summary.setStatus(rs.getString("Status"));
                return summary;
            }, bookingId, accountId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<BookingSummary> findCounterBookingSummary(int bookingId, int staffId) {
        String sql = """
                SELECT b.BookingID, b.BookingCode, b.TotalAmount, b.[Status],
                       m.Title, r.RoomName, st.StartTime,
                       seats.SeatLabels, seats.SeatCount, seats.SeatTotal,
                       COALESCE(food.FoodTotal, 0) AS FoodTotal,
                       seats.HeldUntil
                FROM dbo.Booking b
                JOIN dbo.Showtime st ON st.ShowtimeID = b.ShowtimeID
                JOIN dbo.Movie m ON m.MovieID = st.MovieID
                JOIN dbo.Room r ON r.RoomID = st.RoomID
                CROSS APPLY (
                    SELECT STRING_AGG(CONCAT(s.RowLabel, s.SeatNumber), ', ') AS SeatLabels,
                           COUNT(*) AS SeatCount, COALESCE(SUM(bs.UnitPrice), 0) AS SeatTotal,
                           MIN(bs.HeldUntil) AS HeldUntil
                    FROM dbo.BookingSeat bs
                    JOIN dbo.Seat s ON s.SeatID = bs.SeatID
                    WHERE bs.BookingID = b.BookingID
                ) seats
                OUTER APPLY (
                    SELECT SUM(bfb.Quantity * bfb.UnitPrice) AS FoodTotal
                    FROM dbo.BookingFoodBeverage bfb
                    WHERE bfb.BookingID = b.BookingID
                ) food
                WHERE b.BookingID = ? AND b.StaffID = ? AND UPPER(b.BookingType) = 'COUNTER'
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            BookingSummary summary = new BookingSummary();
            summary.setBookingId(rs.getInt("BookingID"));
            summary.setBookingCode(rs.getString("BookingCode"));
            summary.setMovieTitle(rs.getString("Title"));
            summary.setRoomName(rs.getString("RoomName"));
            summary.setStartTime(rs.getObject("StartTime", LocalDateTime.class));
            summary.setSeatLabels(rs.getString("SeatLabels"));
            summary.setSeatCount(rs.getInt("SeatCount"));
            summary.setSeatTotal(rs.getBigDecimal("SeatTotal"));
            summary.setFoodTotal(rs.getBigDecimal("FoodTotal"));
            summary.setTotalAmount(rs.getBigDecimal("TotalAmount"));
            summary.setHeldUntil(rs.getObject("HeldUntil", LocalDateTime.class));
            summary.setStatus(rs.getString("Status"));
            return summary;
        }, bookingId, staffId).stream().findFirst();
    }

    public List<FoodBeverageOption> findFoodItems(int bookingId) {
        String sql = """
                SELECT item.FoodBeverageItemID, item.[Name], item.[Description],
                       item.ItemType, item.Price, item.ImageURL,
                       COALESCE(selected.Quantity, 0) AS Quantity
                FROM dbo.FoodBeverageItem item
                LEFT JOIN dbo.BookingFoodBeverage selected
                    ON selected.FoodBeverageItemID = item.FoodBeverageItemID
                   AND selected.BookingID = ?
                WHERE UPPER(item.[Status]) = 'ACTIVE'
                ORDER BY item.ItemType, item.[Name]
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            FoodBeverageOption item = new FoodBeverageOption();
            item.setItemId(rs.getInt("FoodBeverageItemID"));
            item.setName(rs.getString("Name"));
            item.setDescription(rs.getString("Description"));
            item.setItemType(rs.getString("ItemType"));
            item.setPrice(rs.getBigDecimal("Price"));
            item.setImageUrl(rs.getString("ImageURL"));
            item.setQuantity(rs.getInt("Quantity"));
            return item;
        }, bookingId);
    }

    public void replaceFoodSelections(int bookingId, List<FoodBeverageOption> items) {
        jdbcTemplate.update("DELETE FROM dbo.BookingFoodBeverage WHERE BookingID = ?", bookingId);
        for (FoodBeverageOption item : items) {
            if (item.getQuantity() <= 0) continue;
            jdbcTemplate.update("""
                    INSERT INTO dbo.BookingFoodBeverage
                        (BookingID, FoodBeverageItemID, Quantity, UnitPrice)
                    VALUES (?, ?, ?, ?)
                    """, bookingId, item.getItemId(), item.getQuantity(), item.getPrice());
        }
    }

    public void releaseBooking(int bookingId, int accountId) {
        jdbcTemplate.update("""
                DELETE food
                FROM dbo.BookingFoodBeverage food
                JOIN dbo.Booking b ON b.BookingID = food.BookingID
                JOIN dbo.Customer c ON c.CustomerID = b.CustomerID
                WHERE b.BookingID = ? AND c.AccountID = ? AND b.[Status] = 'PENDING'
                """, bookingId, accountId);
        jdbcTemplate.update("""
                DELETE seat
                FROM dbo.BookingSeat seat
                JOIN dbo.Booking b ON b.BookingID = seat.BookingID
                JOIN dbo.Customer c ON c.CustomerID = b.CustomerID
                WHERE b.BookingID = ? AND c.AccountID = ? AND b.[Status] = 'PENDING'
                """, bookingId, accountId);
        jdbcTemplate.update("""
                UPDATE b SET b.[Status] = 'CANCELLED'
                FROM dbo.Booking b
                JOIN dbo.Customer c ON c.CustomerID = b.CustomerID
                WHERE b.BookingID = ? AND c.AccountID = ? AND b.[Status] = 'PENDING'
                """, bookingId, accountId);
    }

    public void releaseCounterBooking(int bookingId, int staffId) {
        jdbcTemplate.update("""
                DELETE food
                FROM dbo.BookingFoodBeverage food
                JOIN dbo.Booking b ON b.BookingID = food.BookingID
                WHERE b.BookingID = ? AND b.StaffID = ? AND UPPER(b.BookingType) = 'COUNTER'
                  AND UPPER(b.[Status]) = 'PENDING'
                """, bookingId, staffId);
        jdbcTemplate.update("""
                DELETE seat
                FROM dbo.BookingSeat seat
                JOIN dbo.Booking b ON b.BookingID = seat.BookingID
                WHERE b.BookingID = ? AND b.StaffID = ? AND UPPER(b.BookingType) = 'COUNTER'
                  AND UPPER(b.[Status]) = 'PENDING'
                """, bookingId, staffId);
        jdbcTemplate.update("""
                UPDATE dbo.Booking
                SET [Status] = 'CANCELLED'
                WHERE BookingID = ? AND StaffID = ? AND UPPER(BookingType) = 'COUNTER'
                  AND UPPER([Status]) = 'PENDING'
                """, bookingId, staffId);
    }

    /**
     * JOB-01: Releases expired seat holds and updates booking status to EXPIRED.
     * Strictly protects confirmed, paid, or ticketed reservations.
     * Returns the count of released seat holds.
     */
    public int releaseExpiredSeatReservations() {
        // 1. Mark eligible expired bookings as EXPIRED
        jdbcTemplate.update("""
                UPDATE b SET b.[Status] = 'EXPIRED'
                FROM dbo.Booking b
                WHERE UPPER(b.[Status]) IN ('PENDING', 'PAYMENT_PENDING')
                  AND EXISTS (
                      SELECT 1 FROM dbo.BookingSeat bs
                      WHERE bs.BookingID = b.BookingID
                        AND UPPER(bs.[Status]) = 'HELD'
                        AND bs.HeldUntil <= SYSDATETIME()
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM dbo.Payment p
                      WHERE p.BookingID = b.BookingID
                        AND UPPER(p.[Status]) = 'PAID'
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM dbo.Ticket t
                      JOIN dbo.BookingSeat bs0 ON bs0.BookingSeatID = t.BookingSeatID
                      WHERE bs0.BookingID = b.BookingID
                  )
                """);

        // 2. Remove associated F&B selections for expired bookings
        jdbcTemplate.update("""
                DELETE food
                FROM dbo.BookingFoodBeverage food
                JOIN dbo.Booking b ON b.BookingID = food.BookingID
                WHERE b.[Status] = 'EXPIRED'
                """);

        // 3. Release/delete expired BookingSeat records so seats become available again
        return jdbcTemplate.update("""
                DELETE seat
                FROM dbo.BookingSeat seat
                JOIN dbo.Booking b ON b.BookingID = seat.BookingID
                WHERE b.[Status] = 'EXPIRED'
                """);
    }

    public void expireOldHolds() {
        releaseExpiredSeatReservations();
    }

    private ShowtimeOption mapShowtime(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        ShowtimeOption showtime = new ShowtimeOption();
        showtime.setShowtimeId(rs.getInt("ShowtimeID"));
        showtime.setMovieId(rs.getInt("MovieID"));
        showtime.setRoomId(rs.getInt("RoomID"));
        showtime.setMovieTitle(rs.getString("Title"));
        showtime.setPosterUrl(rs.getString("PosterURL"));
        showtime.setAgeRating(rs.getString("AgeRating"));
        showtime.setDuration(rs.getInt("Duration"));
        showtime.setRoomName(rs.getString("RoomName"));
        showtime.setRoomType(rs.getString("RoomType"));
        showtime.setStartTime(rs.getObject("StartTime", LocalDateTime.class));
        showtime.setEndTime(rs.getObject("EndTime", LocalDateTime.class));
        showtime.setBasePrice(rs.getBigDecimal("BasePrice"));
        showtime.setAvailableSeats(rs.getInt("AvailableSeats"));
        return showtime;
    }
}

package com.cinema.dao;

import com.cinema.model.BookingSummary;
import com.cinema.model.FoodBeverageOption;
import com.cinema.model.MovieBookingOption;
import com.cinema.model.SeatOption;
import com.cinema.model.ShowtimeOption;
import com.cinema.model.PaymentView;
import com.cinema.model.CustomerTicket;
import com.cinema.model.TicketVerification;
import com.cinema.model.BookingHistoryItem;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CustomerBookingDAO {

    private final JdbcTemplate jdbcTemplate;

    public CustomerBookingDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureLoyaltySchema();
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
        return findSeats(showtimeId, null);
    }

    public List<SeatOption> findSeats(int showtimeId, Integer currentBookingId) {
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
                             AND b.BookingID <> COALESCE(?, -1)
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
        }, currentBookingId, showtimeId);
    }

    public List<Integer> findBookingSeatIds(int bookingId, int accountId) {
        return jdbcTemplate.queryForList("""
                SELECT bs.SeatID
                FROM dbo.BookingSeat bs
                JOIN dbo.Booking b ON b.BookingID = bs.BookingID
                JOIN dbo.Customer c ON c.CustomerID = b.CustomerID
                WHERE b.BookingID = ? AND c.AccountID = ?
                  AND UPPER(b.[Status]) IN ('PENDING', 'PAYMENT_PENDING')
                  AND UPPER(bs.[Status]) = 'HELD' AND bs.HeldUntil > SYSDATETIME()
                """, Integer.class, bookingId, accountId);
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
                UPDATE p SET p.[Status] = 'CANCELLED', p.UpdatedAt = SYSDATETIME()
                FROM dbo.Payment p
                JOIN dbo.Booking b ON b.BookingID = p.BookingID
                JOIN dbo.Customer c ON c.CustomerID = b.CustomerID
                WHERE b.BookingID = ? AND c.AccountID = ?
                  AND UPPER(b.[Status]) IN ('PENDING', 'PAYMENT_PENDING')
                  AND UPPER(p.[Status]) = 'PENDING'
                """, bookingId, accountId);
        jdbcTemplate.update("""
                DELETE food
                FROM dbo.BookingFoodBeverage food
                JOIN dbo.Booking b ON b.BookingID = food.BookingID
                JOIN dbo.Customer c ON c.CustomerID = b.CustomerID
                WHERE b.BookingID = ? AND c.AccountID = ? AND UPPER(b.[Status]) IN ('PENDING', 'PAYMENT_PENDING')
                """, bookingId, accountId);
        jdbcTemplate.update("""
                DELETE seat
                FROM dbo.BookingSeat seat
                JOIN dbo.Booking b ON b.BookingID = seat.BookingID
                JOIN dbo.Customer c ON c.CustomerID = b.CustomerID
                WHERE b.BookingID = ? AND c.AccountID = ? AND UPPER(b.[Status]) IN ('PENDING', 'PAYMENT_PENDING')
                """, bookingId, accountId);
        jdbcTemplate.update("""
                UPDATE b SET b.[Status] = 'CANCELLED'
                FROM dbo.Booking b
                JOIN dbo.Customer c ON c.CustomerID = b.CustomerID
                WHERE b.BookingID = ? AND c.AccountID = ? AND UPPER(b.[Status]) IN ('PENDING', 'PAYMENT_PENDING')
                """, bookingId, accountId);
    }

    public int createPayment(int bookingId, int accountId, String method, String transactionRef,
                             LocalDateTime expiresAt) {
        Integer id = jdbcTemplate.queryForObject("""
                INSERT INTO dbo.Payment (BookingID, Method, Amount, TransactionRef, [Status], InitiatedAt, ExpiresAt, UpdatedAt)
                OUTPUT inserted.PaymentID
                SELECT b.BookingID, ?, b.TotalAmount, ?, 'PENDING', SYSDATETIME(), ?, SYSDATETIME()
                FROM dbo.Booking b JOIN dbo.Customer c ON c.CustomerID = b.CustomerID
                WHERE b.BookingID = ? AND c.AccountID = ? AND UPPER(b.[Status]) = 'PENDING'
                  AND EXISTS (SELECT 1 FROM dbo.BookingSeat bs WHERE bs.BookingID=b.BookingID AND bs.HeldUntil>SYSDATETIME())
                """, Integer.class, method, transactionRef, expiresAt, bookingId, accountId);
        if (id == null) throw new IllegalStateException("Payment could not be started for this booking.");
        jdbcTemplate.update("UPDATE dbo.Booking SET [Status]='PAYMENT_PENDING' WHERE BookingID=?", bookingId);
        return id;
    }

    public Optional<PaymentView> findPayment(int paymentId, int accountId) {
        return jdbcTemplate.query("""
                SELECT p.PaymentID,p.BookingID,b.BookingCode,p.TransactionRef,p.Method,p.Amount,p.[Status],p.ExpiresAt,
                       (SELECT MIN(bs.HeldUntil) FROM dbo.BookingSeat bs WHERE bs.BookingID=b.BookingID) HeldUntil
                FROM dbo.Payment p JOIN dbo.Booking b ON b.BookingID=p.BookingID
                JOIN dbo.Customer c ON c.CustomerID=b.CustomerID
                WHERE p.PaymentID=? AND c.AccountID=?
                """, (rs,n) -> new PaymentView(rs.getInt("PaymentID"), rs.getInt("BookingID"),
                rs.getString("BookingCode"), rs.getString("TransactionRef"), rs.getString("Method"),
                rs.getBigDecimal("Amount"), rs.getString("Status"),
                rs.getObject("ExpiresAt", LocalDateTime.class), rs.getObject("HeldUntil", LocalDateTime.class)),
                paymentId, accountId).stream().findFirst();
    }

    public Optional<PaymentView> findPaymentByTransactionRef(String transactionRef) {
        if (transactionRef == null || transactionRef.isBlank()) return Optional.empty();
        return jdbcTemplate.query("""
                SELECT p.PaymentID,p.BookingID,b.BookingCode,p.TransactionRef,p.Method,p.Amount,p.[Status],p.ExpiresAt,
                       (SELECT MIN(bs.HeldUntil) FROM dbo.BookingSeat bs WHERE bs.BookingID=b.BookingID) HeldUntil
                FROM dbo.Payment p JOIN dbo.Booking b ON b.BookingID=p.BookingID
                WHERE p.TransactionRef=?
                """, (rs,n) -> new PaymentView(rs.getInt("PaymentID"), rs.getInt("BookingID"),
                rs.getString("BookingCode"), rs.getString("TransactionRef"), rs.getString("Method"),
                rs.getBigDecimal("Amount"), rs.getString("Status"),
                rs.getObject("ExpiresAt", LocalDateTime.class), rs.getObject("HeldUntil", LocalDateTime.class)),
                transactionRef).stream().findFirst();
    }

    public void applyVnpayResult(int paymentId, boolean successful, String providerTransactionNo,
                                 String responseCode, String transactionStatus, String bankCode, String cardType) {
        PaymentView payment = findPaymentById(paymentId)
                .orElseThrow(() -> new IllegalStateException("The payment transaction could not be found."));
        if (!"PENDING".equalsIgnoreCase(payment.status())) return;
        if (!successful) {
            jdbcTemplate.update("""
                    UPDATE dbo.Payment SET [Status]='FAILED',ProviderTransactionNo=?,ResponseCode=?,
                    TransactionStatus=?,BankCode=?,CardType=?,LastCallbackAt=SYSDATETIME(),UpdatedAt=SYSDATETIME()
                    WHERE PaymentID=? AND [Status]='PENDING'
                    """, providerTransactionNo, responseCode, transactionStatus, bankCode, cardType, paymentId);
            return;
        }
        jdbcTemplate.update("""
                UPDATE dbo.Payment SET [Status]='PAID',ProviderTransactionNo=?,ResponseCode=?,TransactionStatus=?,
                BankCode=?,CardType=?,PaidAt=SYSDATETIME(),LastCallbackAt=SYSDATETIME(),UpdatedAt=SYSDATETIME()
                WHERE PaymentID=? AND [Status]='PENDING'
                """, providerTransactionNo, responseCode, transactionStatus, bankCode, cardType, paymentId);
        confirmBookingAndIssueTickets(payment.bookingId());
    }

    private Optional<PaymentView> findPaymentById(int paymentId) {
        return jdbcTemplate.query("""
                SELECT p.PaymentID,p.BookingID,b.BookingCode,p.TransactionRef,p.Method,p.Amount,p.[Status],p.ExpiresAt,
                       (SELECT MIN(bs.HeldUntil) FROM dbo.BookingSeat bs WHERE bs.BookingID=b.BookingID) HeldUntil
                FROM dbo.Payment p JOIN dbo.Booking b ON b.BookingID=p.BookingID WHERE p.PaymentID=?
                """, (rs,n) -> new PaymentView(rs.getInt("PaymentID"), rs.getInt("BookingID"),
                rs.getString("BookingCode"), rs.getString("TransactionRef"), rs.getString("Method"),
                rs.getBigDecimal("Amount"), rs.getString("Status"),
                rs.getObject("ExpiresAt", LocalDateTime.class), rs.getObject("HeldUntil", LocalDateTime.class)),
                paymentId).stream().findFirst();
    }

    public void expirePaymentIfNeeded(int paymentId, int accountId) {
        jdbcTemplate.update("""
                UPDATE p SET p.[Status]='EXPIRED',p.UpdatedAt=SYSDATETIME()
                FROM dbo.Payment p JOIN dbo.Booking b ON b.BookingID=p.BookingID
                JOIN dbo.Customer c ON c.CustomerID=b.CustomerID
                WHERE p.PaymentID=? AND c.AccountID=? AND p.[Status]='PENDING' AND p.ExpiresAt<=SYSDATETIME()
                """, paymentId, accountId);
    }

    public int regeneratePayment(int oldPaymentId, int accountId, String transactionRef, LocalDateTime expiresAt) {
        PaymentView old = findPayment(oldPaymentId, accountId)
                .orElseThrow(() -> new IllegalStateException("The payment transaction could not be found."));
        if (old.heldUntil() == null || !old.heldUntil().isAfter(LocalDateTime.now()))
            throw new IllegalStateException("Your seat hold has expired. Please select your seats again.");
        jdbcTemplate.update("UPDATE dbo.Payment SET [Status]='EXPIRED',UpdatedAt=SYSDATETIME() WHERE PaymentID=? AND [Status]='PENDING'", oldPaymentId);
        Integer id = jdbcTemplate.queryForObject("""
                INSERT INTO dbo.Payment (BookingID,Method,Amount,TransactionRef,[Status],InitiatedAt,ExpiresAt,UpdatedAt)
                OUTPUT inserted.PaymentID SELECT BookingID,Method,Amount,?,'PENDING',SYSDATETIME(),?,SYSDATETIME()
                FROM dbo.Payment WHERE PaymentID=?
                """, Integer.class, transactionRef, expiresAt, oldPaymentId);
        return id == null ? 0 : id;
    }

    public void confirmSimulatedPayment(int paymentId, int accountId) {
        PaymentView payment = findPayment(paymentId, accountId)
                .orElseThrow(() -> new IllegalStateException("The payment transaction could not be found."));
        LocalDateTime now = LocalDateTime.now();
        if (!"PENDING".equalsIgnoreCase(payment.status()) || payment.expiresAt() == null || !payment.expiresAt().isAfter(now))
            throw new IllegalStateException("The payment code has expired. Please generate a new code.");
        if (payment.heldUntil() == null || !payment.heldUntil().isAfter(now))
            throw new IllegalStateException("Your seat hold has expired. Please select your seats again.");
        jdbcTemplate.update("UPDATE dbo.Payment SET [Status]='PAID',PaidAt=SYSDATETIME(),UpdatedAt=SYSDATETIME(),ResponseCode='00',TransactionStatus='SUCCESS' WHERE PaymentID=? AND [Status]='PENDING'", paymentId);
        confirmBookingAndIssueTickets(payment.bookingId());
    }

    private void confirmBookingAndIssueTickets(int bookingId) {
        jdbcTemplate.update("UPDATE dbo.Booking SET [Status]='CONFIRMED' WHERE BookingID=? AND [Status]='PAYMENT_PENDING'", bookingId);
        jdbcTemplate.update("UPDATE dbo.BookingSeat SET [Status]='BOOKED',HeldUntil=NULL WHERE BookingID=? AND [Status]='HELD'", bookingId);
        List<Integer> bookingSeatIds = jdbcTemplate.queryForList("SELECT BookingSeatID FROM dbo.BookingSeat WHERE BookingID=?", Integer.class, bookingId);
        for (Integer bookingSeatId : bookingSeatIds) {
            jdbcTemplate.update("""
                    IF NOT EXISTS (SELECT 1 FROM dbo.Ticket WHERE BookingSeatID=?)
                    INSERT dbo.Ticket(BookingSeatID,QRCode,TicketStatus) VALUES (?,?,'VALID')
                    """, bookingSeatId, bookingSeatId, "CINEORA-" + UUID.randomUUID());
        }
        awardLoyaltyPoints(bookingId);
    }

    private void awardLoyaltyPoints(int bookingId) {
        jdbcTemplate.update("""
                DECLARE @Award TABLE (CustomerID INT, Points INT);
                INSERT dbo.LoyaltyTransaction(CustomerID,BookingID,Points,TransactionType,[Description])
                OUTPUT inserted.CustomerID,inserted.Points INTO @Award(CustomerID,Points)
                SELECT b.CustomerID,b.BookingID,
                       CONVERT(INT,FLOOR((b.TotalAmount / 10000.0) * COALESCE(
                           CASE WHEN ISJSON(mr.Benefits)=1 THEN TRY_CONVERT(DECIMAL(8,2),JSON_VALUE(mr.Benefits,'$.pointsRate')) END,1))),
                       'EARN',CONCAT(N'Points earned from booking ',b.BookingCode)
                FROM dbo.Booking b
                JOIN dbo.Customer c ON c.CustomerID=b.CustomerID
                LEFT JOIN dbo.MembershipRank mr ON mr.RankID=c.RankID
                WHERE b.BookingID=? AND UPPER(b.[Status])='CONFIRMED' AND b.TotalAmount>=10000
                  AND EXISTS (SELECT 1 FROM dbo.Payment p WHERE p.BookingID=b.BookingID AND UPPER(p.[Status])='PAID')
                  AND NOT EXISTS (SELECT 1 FROM dbo.LoyaltyTransaction lt WHERE lt.BookingID=b.BookingID AND lt.TransactionType='EARN');

                UPDATE c SET c.AvailablePoints=c.AvailablePoints+a.Points,c.TierPoints=c.TierPoints+a.Points
                FROM dbo.Customer c JOIN @Award a ON a.CustomerID=c.CustomerID;

                UPDATE c SET RankID=ranked.RankID
                FROM dbo.Customer c JOIN @Award a ON a.CustomerID=c.CustomerID
                CROSS APPLY (SELECT TOP 1 mr.RankID FROM dbo.MembershipRank mr
                             WHERE mr.IsActive=1 AND mr.MinPoints<=c.TierPoints ORDER BY mr.MinPoints DESC) ranked;
                """, bookingId);
    }

    private void ensureLoyaltySchema() {
        try {
            jdbcTemplate.execute("""
                    IF OBJECT_ID(N'dbo.LoyaltyTransaction',N'U') IS NULL
                    BEGIN
                      CREATE TABLE dbo.LoyaltyTransaction(
                        LoyaltyTransactionID INT IDENTITY(1,1) PRIMARY KEY,CustomerID INT NOT NULL,
                        BookingID INT NULL,Points INT NOT NULL,TransactionType VARCHAR(20) NOT NULL,
                        [Description] NVARCHAR(250) NOT NULL,CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                        CONSTRAINT FK_LoyaltyTx_Customer FOREIGN KEY(CustomerID) REFERENCES dbo.Customer(CustomerID),
                        CONSTRAINT FK_LoyaltyTx_Booking FOREIGN KEY(BookingID) REFERENCES dbo.Booking(BookingID));
                      CREATE UNIQUE INDEX UX_LoyaltyTransaction_BookingEarn ON dbo.LoyaltyTransaction(BookingID,TransactionType)
                        WHERE BookingID IS NOT NULL AND TransactionType='EARN';
                    END
                    """);
        } catch (DataAccessException ignored) {
            // Managed deployments create this table through 00_full_database.sql.
        }
    }

    public List<CustomerTicket> findTicketsByBooking(int bookingId, int accountId) {
        return jdbcTemplate.query("""
                SELECT t.TicketID,t.QRCode,t.TicketStatus,b.BookingCode,m.Title,m.PosterURL,r.RoomName,st.StartTime,
                       CONCAT(s.RowLabel,s.SeatNumber) SeatLabel
                FROM dbo.Ticket t JOIN dbo.BookingSeat bs ON bs.BookingSeatID=t.BookingSeatID
                JOIN dbo.Booking b ON b.BookingID=bs.BookingID JOIN dbo.Customer c ON c.CustomerID=b.CustomerID
                JOIN dbo.Showtime st ON st.ShowtimeID=b.ShowtimeID JOIN dbo.Movie m ON m.MovieID=st.MovieID
                JOIN dbo.Room r ON r.RoomID=st.RoomID JOIN dbo.Seat s ON s.SeatID=bs.SeatID
                WHERE b.BookingID=? AND c.AccountID=? ORDER BY s.GridRow,s.GridColumn
                """, (rs,n) -> new CustomerTicket(rs.getInt("TicketID"),rs.getString("QRCode"),rs.getString("TicketStatus"),
                rs.getString("BookingCode"),rs.getString("Title"),rs.getString("RoomName"),
                rs.getObject("StartTime",LocalDateTime.class),rs.getString("SeatLabel"),rs.getString("PosterURL")), bookingId, accountId);
    }

    public Optional<TicketVerification> findTicketByQrCode(String qrCode) {
        if (qrCode == null || qrCode.isBlank()) return Optional.empty();
        return jdbcTemplate.query("""
                SELECT t.TicketID,t.QRCode,t.TicketStatus,b.BookingCode,a.FullName,a.Email,a.Phone,
                       m.Title,r.RoomName,st.StartTime,CONCAT(s.RowLabel,s.SeatNumber) SeatLabel
                FROM dbo.Ticket t
                JOIN dbo.BookingSeat bs ON bs.BookingSeatID=t.BookingSeatID
                JOIN dbo.Booking b ON b.BookingID=bs.BookingID
                JOIN dbo.Customer c ON c.CustomerID=b.CustomerID
                JOIN dbo.Account a ON a.AccountID=c.AccountID
                JOIN dbo.Showtime st ON st.ShowtimeID=b.ShowtimeID
                JOIN dbo.Movie m ON m.MovieID=st.MovieID
                JOIN dbo.Room r ON r.RoomID=st.RoomID
                JOIN dbo.Seat s ON s.SeatID=bs.SeatID
                WHERE t.QRCode=?
                """, (rs,n) -> new TicketVerification(rs.getInt("TicketID"),rs.getString("QRCode"),
                rs.getString("TicketStatus"),rs.getString("BookingCode"),rs.getString("FullName"),
                rs.getString("Email"),rs.getString("Phone"),rs.getString("Title"),rs.getString("RoomName"),
                rs.getObject("StartTime",LocalDateTime.class),rs.getString("SeatLabel")), qrCode).stream().findFirst();
    }

    public List<BookingHistoryItem> findBookingHistory(int accountId) {
        return jdbcTemplate.query("""
                SELECT b.BookingID,b.BookingCode,m.Title,m.PosterURL,r.RoomName,st.StartTime,b.TotalAmount,b.[Status],
                       COALESCE((SELECT TOP 1 p.[Status] FROM dbo.Payment p WHERE p.BookingID=b.BookingID ORDER BY p.PaymentID DESC),'UNPAID') PaymentStatus,
                       (SELECT STRING_AGG(CONCAT(s.RowLabel,s.SeatNumber),', ') FROM dbo.BookingSeat bs JOIN dbo.Seat s ON s.SeatID=bs.SeatID WHERE bs.BookingID=b.BookingID) SeatLabels
                FROM dbo.Booking b JOIN dbo.Customer c ON c.CustomerID=b.CustomerID
                JOIN dbo.Showtime st ON st.ShowtimeID=b.ShowtimeID JOIN dbo.Movie m ON m.MovieID=st.MovieID
                JOIN dbo.Room r ON r.RoomID=st.RoomID WHERE c.AccountID=? ORDER BY b.BookingTime DESC
                """, (rs,n) -> new BookingHistoryItem(rs.getInt("BookingID"),rs.getString("BookingCode"),rs.getString("Title"),
                rs.getString("PosterURL"),rs.getString("RoomName"),rs.getObject("StartTime",LocalDateTime.class),rs.getString("SeatLabels"),
                rs.getBigDecimal("TotalAmount"),rs.getString("Status"),rs.getString("PaymentStatus")), accountId);
    }

    public void releaseCounterBooking(int bookingId, int staffId) {
        cancelPendingCounterPayments(bookingId, staffId);
        jdbcTemplate.update("""
                DELETE food
                FROM dbo.BookingFoodBeverage food
                JOIN dbo.Booking b ON b.BookingID = food.BookingID
                WHERE b.BookingID = ? AND b.StaffID = ? AND UPPER(b.BookingType) = 'COUNTER'
                  AND UPPER(b.[Status]) IN ('PENDING', 'PAYMENT_PENDING')
                """, bookingId, staffId);
        jdbcTemplate.update("""
                DELETE seat
                FROM dbo.BookingSeat seat
                JOIN dbo.Booking b ON b.BookingID = seat.BookingID
                WHERE b.BookingID = ? AND b.StaffID = ? AND UPPER(b.BookingType) = 'COUNTER'
                  AND UPPER(b.[Status]) IN ('PENDING', 'PAYMENT_PENDING')
                """, bookingId, staffId);
        jdbcTemplate.update("""
                UPDATE dbo.Booking
                SET [Status] = 'CANCELLED'
                WHERE BookingID = ? AND StaffID = ? AND UPPER(BookingType) = 'COUNTER'
                  AND UPPER([Status]) IN ('PENDING', 'PAYMENT_PENDING')
                """, bookingId, staffId);
    }

    public Optional<String> findBookingAgeRating(int bookingId) {
        return jdbcTemplate.query("""
                SELECT m.AgeRating
                FROM dbo.Booking b
                JOIN dbo.Showtime st ON st.ShowtimeID = b.ShowtimeID
                JOIN dbo.Movie m ON m.MovieID = st.MovieID
                WHERE b.BookingID = ?
                """, (rs, rowNum) -> rs.getString("AgeRating"), bookingId).stream().findFirst();
    }

    /**
     * Starts a counter payment attempt for the booking's current total (BR-24). Any earlier
     * unfinished attempt is cancelled first so at most one attempt can succeed (BR-41).
     */
    public Optional<Integer> createCounterPayment(int bookingId, int staffId, String method,
                                                  String transactionRef, LocalDateTime expiresAt) {
        cancelPendingCounterPayments(bookingId, staffId);
        Optional<Integer> paymentId = jdbcTemplate.query("""
                INSERT INTO dbo.Payment (BookingID, Method, Amount, TransactionRef, [Status], InitiatedAt, ExpiresAt, UpdatedAt)
                OUTPUT inserted.PaymentID
                SELECT b.BookingID, ?, b.TotalAmount, ?, 'PENDING', SYSDATETIME(), ?, SYSDATETIME()
                FROM dbo.Booking b
                WHERE b.BookingID = ? AND b.StaffID = ? AND UPPER(b.BookingType) = 'COUNTER'
                  AND UPPER(b.[Status]) IN ('PENDING', 'PAYMENT_PENDING')
                  AND EXISTS (SELECT 1 FROM dbo.BookingSeat bs
                              WHERE bs.BookingID = b.BookingID AND bs.HeldUntil > SYSDATETIME())
                """, (rs, rowNum) -> rs.getInt("PaymentID"),
                method, transactionRef, expiresAt, bookingId, staffId).stream().findFirst();
        paymentId.ifPresent(id -> jdbcTemplate.update(
                "UPDATE dbo.Booking SET [Status]='PAYMENT_PENDING' WHERE BookingID=? AND [Status]='PENDING'", bookingId));
        return paymentId;
    }

    /** Marks a pending counter payment as paid, then confirms the booking and issues tickets (BR-22, BR-23). */
    public void confirmCounterPayment(int paymentId) {
        PaymentView payment = findPaymentById(paymentId)
                .orElseThrow(() -> new IllegalStateException("The payment transaction could not be found."));
        int updated = jdbcTemplate.update("""
                UPDATE dbo.Payment SET [Status]='PAID',PaidAt=SYSDATETIME(),UpdatedAt=SYSDATETIME(),TransactionStatus='SUCCESS'
                WHERE PaymentID=? AND [Status]='PENDING' AND (ExpiresAt IS NULL OR ExpiresAt>SYSDATETIME())
                """, paymentId);
        if (updated == 0) throw new IllegalStateException("The payment is no longer pending. Start the payment again.");
        confirmBookingAndIssueTickets(payment.bookingId());
    }

    public Optional<PaymentView> findLatestCounterPayment(int bookingId, int staffId) {
        jdbcTemplate.update("""
                UPDATE p SET p.[Status]='EXPIRED',p.UpdatedAt=SYSDATETIME()
                FROM dbo.Payment p JOIN dbo.Booking b ON b.BookingID=p.BookingID
                WHERE p.BookingID=? AND b.StaffID=? AND p.[Status]='PENDING' AND p.ExpiresAt<=SYSDATETIME()
                """, bookingId, staffId);
        return jdbcTemplate.query("""
                SELECT TOP 1 p.PaymentID,p.BookingID,b.BookingCode,p.TransactionRef,p.Method,p.Amount,p.[Status],p.ExpiresAt,
                       (SELECT MIN(bs.HeldUntil) FROM dbo.BookingSeat bs WHERE bs.BookingID=b.BookingID) HeldUntil
                FROM dbo.Payment p JOIN dbo.Booking b ON b.BookingID=p.BookingID
                WHERE p.BookingID=? AND b.StaffID=? AND UPPER(b.BookingType)='COUNTER'
                ORDER BY p.PaymentID DESC
                """, (rs,n) -> new PaymentView(rs.getInt("PaymentID"), rs.getInt("BookingID"),
                rs.getString("BookingCode"), rs.getString("TransactionRef"), rs.getString("Method"),
                rs.getBigDecimal("Amount"), rs.getString("Status"),
                rs.getObject("ExpiresAt", LocalDateTime.class), rs.getObject("HeldUntil", LocalDateTime.class)),
                bookingId, staffId).stream().findFirst();
    }

    private void cancelPendingCounterPayments(int bookingId, int staffId) {
        jdbcTemplate.update("""
                UPDATE p SET p.[Status]='CANCELLED',p.UpdatedAt=SYSDATETIME()
                FROM dbo.Payment p JOIN dbo.Booking b ON b.BookingID=p.BookingID
                WHERE p.BookingID=? AND b.StaffID=? AND UPPER(b.BookingType)='COUNTER' AND p.[Status]='PENDING'
                """, bookingId, staffId);
    }

    /**
     * JOB-01: Releases expired seat holds and updates booking status to EXPIRED.
     * Strictly protects confirmed, paid, or ticketed reservations.
     * Returns the count of released seat holds.
     */
    public int releaseExpiredSeatReservations() {
        // 1. Expire pending payment attempts whose QR/session deadline passed.
        jdbcTemplate.update("""
                UPDATE dbo.Payment SET [Status]='EXPIRED', UpdatedAt=SYSDATETIME()
                WHERE UPPER([Status])='PENDING' AND ExpiresAt IS NOT NULL AND ExpiresAt<=SYSDATETIME()
                """);

        // 2. Mark eligible expired bookings as EXPIRED
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

        // 3. Remove associated F&B selections for expired bookings
        jdbcTemplate.update("""
                DELETE food
                FROM dbo.BookingFoodBeverage food
                JOIN dbo.Booking b ON b.BookingID = food.BookingID
                WHERE b.[Status] = 'EXPIRED'
                """);

        // 4. Release/delete expired BookingSeat records so seats become available again
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

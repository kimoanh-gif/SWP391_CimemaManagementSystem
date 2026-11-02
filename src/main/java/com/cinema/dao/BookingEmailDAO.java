package com.cinema.dao;

import com.cinema.model.BookingEmailDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Read-only queries against existing booking/payment tables; no schema changes. */
@Repository
public class BookingEmailDAO {
    private final JdbcTemplate jdbc;

    public BookingEmailDAO(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final String ELIGIBLE = """
            UPPER(b.[Status]) = 'CONFIRMED' AND UPPER(b.BookingType) = 'ONLINE'
            AND UPPER(st.[Status]) NOT IN ('CANCELLED', 'INACTIVE', 'DISABLED')
            AND EXISTS (SELECT 1 FROM dbo.Payment p
                        WHERE p.BookingID=b.BookingID AND UPPER(p.[Status])='PAID')
            """;

    public Optional<BookingEmailDetails> findPaidBooking(int bookingId) {
        return jdbc.query("""
                SELECT b.BookingID,b.BookingCode,a.Email,a.FullName,m.Title,r.RoomName,st.StartTime,b.TotalAmount,
                    (SELECT STRING_AGG(CONCAT(s.RowLabel,s.SeatNumber), ', ')
                            WITHIN GROUP (ORDER BY s.RowLabel,s.SeatNumber)
                     FROM dbo.BookingSeat bs JOIN dbo.Seat s ON s.SeatID=bs.SeatID
                     WHERE bs.BookingID=b.BookingID AND UPPER(bs.[Status])='BOOKED') Seats,
                    (SELECT STRING_AGG(CONCAT(bfb.Quantity, N' x ', fbi.[Name]), N', ')
                     FROM dbo.BookingFoodBeverage bfb
                     JOIN dbo.FoodBeverageItem fbi ON fbi.FoodBeverageItemID=bfb.FoodBeverageItemID
                     WHERE bfb.BookingID=b.BookingID AND bfb.Quantity > 0) FoodAndDrinks
                FROM dbo.Booking b JOIN dbo.Customer c ON c.CustomerID=b.CustomerID
                JOIN dbo.Account a ON a.AccountID=c.AccountID
                JOIN dbo.Showtime st ON st.ShowtimeID=b.ShowtimeID
                JOIN dbo.Movie m ON m.MovieID=st.MovieID JOIN dbo.Room r ON r.RoomID=st.RoomID
                WHERE b.BookingID=? AND
                """ + ELIGIBLE, (rs, n) -> new BookingEmailDetails(rs.getInt("BookingID"),
                rs.getString("BookingCode"), rs.getString("Email"), rs.getString("FullName"),
                rs.getString("Title"), rs.getString("RoomName"), rs.getObject("StartTime", LocalDateTime.class),
                rs.getString("Seats"), rs.getString("FoodAndDrinks"), rs.getBigDecimal("TotalAmount")), bookingId).stream().findFirst();
    }

    /** Recovery for missed after-commit events and temporary SMTP failures. */
    public List<Integer> findRecentlyPaidBookingIds() {
        return jdbc.queryForList("""
                SELECT b.BookingID FROM dbo.Booking b
                JOIN dbo.Showtime st ON st.ShowtimeID=b.ShowtimeID
                WHERE
                """ + ELIGIBLE + """
                AND EXISTS (SELECT 1 FROM dbo.Payment p WHERE p.BookingID=b.BookingID
                            AND UPPER(p.[Status])='PAID'
                            AND COALESCE(p.PaidAt,p.CreatedAt)>=DATEADD(HOUR,-24,SYSDATETIME()))
                ORDER BY b.BookingID
                """, Integer.class);
    }
}

package com.cinema.dao;
import com.cinema.model.MembershipDiscountPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.Optional;

@Repository
public class MembershipDiscountDAO {
    private final JdbcTemplate jdbc;
    public MembershipDiscountDAO(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Order(int customerId, int rankId, String status, Integer voucherId,
                        BigDecimal subtotal, MembershipDiscountPolicy policy) {}
    /** Must run within the service transaction: customer lock serializes concurrent checkouts. */
    public Optional<Order> lockOrder(int bookingId, int ownerId, boolean counter) {
        String owner = counter ? "b.StaffID=? AND UPPER(b.BookingType)='COUNTER'" : "c.AccountID=?";
        var customers = jdbc.queryForList("SELECT c.CustomerID FROM dbo.Booking b JOIN dbo.Customer c ON c.CustomerID=b.CustomerID WHERE b.BookingID=? AND " + owner,
                Integer.class, bookingId, ownerId);
        if (customers.isEmpty()) return Optional.empty();
        jdbc.queryForList("SELECT CustomerID FROM dbo.Customer WITH (UPDLOCK,HOLDLOCK) WHERE CustomerID=?", customers.get(0));
        return jdbc.query("""
                SELECT b.CustomerID,b.[Status],b.Subtotal,b.VoucherID,
                       mr.RankID,mr.DiscountUsageLimit,mr.MinBookingAmount,mr.MaxDiscountAmount,mr.DiscountPercent
                FROM dbo.Booking b WITH (UPDLOCK,HOLDLOCK) JOIN dbo.Customer c ON c.CustomerID=b.CustomerID
                OUTER APPLY (SELECT TOP 1 * FROM dbo.MembershipRank r WHERE r.IsActive=1
                    AND r.MinPoints<=c.TierPoints ORDER BY r.MinPoints DESC,r.RankID) mr
                WHERE b.BookingID=? AND
                """ + owner, (rs, n) -> {
            int voucher = rs.getInt("VoucherID");
            Integer voucherId = rs.wasNull() ? null : voucher;
            return new Order(rs.getInt("CustomerID"), rs.getInt("RankID"), rs.getString("Status"), voucherId,
                    rs.getBigDecimal("Subtotal"), new MembershipDiscountPolicy(rs.getInt("DiscountUsageLimit"),
                    rs.getBigDecimal("MinBookingAmount"), rs.getBigDecimal("MaxDiscountAmount"), rs.getBigDecimal("DiscountPercent")));
        }, bookingId, ownerId).stream().findFirst();
    }
    public int countUsedOrReserved(int customer, int rank, int excludeBooking) {
        var count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM dbo.MembershipDiscountUsage u JOIN dbo.Booking b ON b.BookingID=u.BookingID
                WHERE u.CustomerID=? AND u.RankID=? AND u.BookingID<>? AND (
                    EXISTS (SELECT 1 FROM dbo.Payment p WHERE p.BookingID=b.BookingID AND UPPER(p.[Status])='PAID')
                    OR (UPPER(b.[Status]) IN ('PENDING','PAYMENT_PENDING') AND (
                        EXISTS (SELECT 1 FROM dbo.Payment p WHERE p.BookingID=b.BookingID AND UPPER(p.[Status])='PENDING')
                        OR EXISTS (SELECT 1 FROM dbo.BookingSeat s WHERE s.BookingID=b.BookingID AND s.HeldUntil>SYSDATETIME()))))
                """, Integer.class, customer, rank, excludeBooking);
        return count == null ? 0 : count;
    }
    public void saveReservation(int booking, Order order, BigDecimal discount) {
        jdbc.update("DELETE FROM dbo.MembershipDiscountUsage WHERE BookingID=?", booking);
        if (order.voucherId() != null) return;
        if (discount.signum() > 0) jdbc.update("INSERT dbo.MembershipDiscountUsage(BookingID,CustomerID,RankID,DiscountAmount) VALUES (?,?,?,?)",
                booking, order.customerId(), order.rankId(), discount);
        jdbc.update("UPDATE dbo.Booking SET DiscountAmount=?,TotalAmount=Subtotal-? WHERE BookingID=? AND UPPER([Status])='PENDING'", discount, discount, booking);
    }
}

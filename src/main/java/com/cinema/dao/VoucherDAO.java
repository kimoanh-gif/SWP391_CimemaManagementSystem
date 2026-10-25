package com.cinema.dao;

import com.cinema.model.BookingPricing;
import com.cinema.model.VoucherCandidate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO for the Voucher / Promotion Selection step and Loyalty voucher list.
 * Assignee: Nguyễn Đức Anh (Iter3)
 */
@Repository
public class VoucherDAO {

    private static final String CANDIDATE_SELECT = """
            SELECT v.VoucherID, v.Code, v.DiscountType, v.DiscountValue, v.MaxDiscountAmount,
                   v.MinBookingAmount, v.ValidFrom, v.ValidTo, v.TotalUsageLimit, v.PerAccountLimit,
                   v.[Status],
                   p.[Name] AS PromotionName, p.[Description] AS PromotionDescription,
                   p.[Status] AS PromotionStatus, p.StartDate AS PromotionStart, p.EndDate AS PromotionEnd,
                   v.RequiredRankID,
                   COALESCE(r.DisplayName, r.[Name]) AS RequiredRankName,
                   r.MinPoints AS RequiredRankMinPoints
            FROM dbo.Voucher v
            JOIN dbo.Promotion p ON p.PromotionID = v.PromotionID
            LEFT JOIN dbo.MembershipRank r ON r.RankID = v.RequiredRankID
            """;

    /** Usage counts every Booking that still holds or has used the voucher. */
    private static final String USED_BOOKING_STATUS = "UPPER(b.[Status]) NOT IN ('CANCELLED', 'EXPIRED')";

    private final JdbcTemplate jdbcTemplate;

    public VoucherDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<VoucherCandidate> candidateMapper = (rs, rowNum) -> new VoucherCandidate(
            rs.getInt("VoucherID"),
            rs.getString("Code"),
            rs.getString("DiscountType"),
            rs.getBigDecimal("DiscountValue"),
            rs.getBigDecimal("MaxDiscountAmount"),
            rs.getBigDecimal("MinBookingAmount"),
            toLocalDateTime(rs, "ValidFrom"),
            toLocalDateTime(rs, "ValidTo"),
            nullableInt(rs, "TotalUsageLimit"),
            nullableInt(rs, "PerAccountLimit"),
            rs.getString("Status"),
            rs.getString("PromotionName"),
            rs.getString("PromotionDescription"),
            rs.getString("PromotionStatus"),
            toLocalDateTime(rs, "PromotionStart"),
            toLocalDateTime(rs, "PromotionEnd"),
            nullableInt(rs, "RequiredRankID"),
            rs.getString("RequiredRankName"),
            nullableInt(rs, "RequiredRankMinPoints")
    );

    /** Voucher by code (case-insensitive), whatever its status – the service decides validity. */
    public Optional<VoucherCandidate> findByCode(String code) {
        String sql = CANDIDATE_SELECT + " WHERE UPPER(v.Code) = UPPER(?)";
        return jdbcTemplate.query(sql, candidateMapper, code).stream().findFirst();
    }

    public Optional<VoucherCandidate> findById(int voucherId) {
        String sql = CANDIDATE_SELECT + " WHERE v.VoucherID = ?";
        return jdbcTemplate.query(sql, candidateMapper, voucherId).stream().findFirst();
    }

    /** Vouchers that are active and inside both the voucher and promotion validity windows. */
    public List<VoucherCandidate> findCurrentlyValid() {
        String sql = CANDIDATE_SELECT + """
                WHERE UPPER(v.[Status]) = 'ACTIVE' AND UPPER(p.[Status]) = 'ACTIVE'
                  AND v.ValidFrom <= SYSDATETIME() AND v.ValidTo >= SYSDATETIME()
                  AND p.StartDate <= SYSDATETIME() AND p.EndDate >= SYSDATETIME()
                ORDER BY v.ValidTo, v.VoucherID
                """;
        return jdbcTemplate.query(sql, candidateMapper);
    }

    /** BR-19: how many bookings (other than this one) use the voucher. */
    public int countUsage(int voucherId, int excludeBookingId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM dbo.Booking b
                WHERE b.VoucherID = ? AND b.BookingID <> ? AND
                """ + USED_BOOKING_STATUS, Integer.class, voucherId, excludeBookingId);
        return count != null ? count : 0;
    }

    /** BR-38: how many bookings (other than this one) of this Customer use the voucher. */
    public int countCustomerUsage(int voucherId, int customerId, int excludeBookingId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM dbo.Booking b
                WHERE b.VoucherID = ? AND b.CustomerID = ? AND b.BookingID <> ? AND
                """ + USED_BOOKING_STATUS, Integer.class, voucherId, customerId, excludeBookingId);
        return count != null ? count : 0;
    }

    /** Booking money + voucher state; only returns the booking if it belongs to the account. */
    public Optional<BookingPricing> findBookingPricing(int bookingId, int accountId) {
        String sql = """
                SELECT b.BookingID, b.CustomerID, c.TierPoints, b.[Status],
                       b.Subtotal, b.DiscountAmount, b.TotalAmount, b.VoucherID, v.Code AS VoucherCode
                FROM dbo.Booking b
                JOIN dbo.Customer c ON c.CustomerID = b.CustomerID
                LEFT JOIN dbo.Voucher v ON v.VoucherID = b.VoucherID
                WHERE b.BookingID = ? AND c.AccountID = ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new BookingPricing(
                rs.getInt("BookingID"),
                rs.getInt("CustomerID"),
                rs.getInt("TierPoints"),
                rs.getString("Status"),
                rs.getBigDecimal("Subtotal"),
                rs.getBigDecimal("DiscountAmount"),
                rs.getBigDecimal("TotalAmount"),
                nullableInt(rs, "VoucherID"),
                rs.getString("VoucherCode")
        ), bookingId, accountId).stream().findFirst();
    }

    /**
     * Applies (voucherId != null) or removes (voucherId == null) the voucher of a PENDING booking.
     * TotalAmount = Subtotal − DiscountAmount (BR-16). BR-21: the single VoucherID column means one voucher per booking.
     */
    public int updateBookingVoucher(int bookingId, Integer voucherId, BigDecimal discountAmount) {
        return jdbcTemplate.update("""
                UPDATE dbo.Booking
                SET VoucherID = ?, DiscountAmount = ?, TotalAmount = Subtotal - ?
                WHERE BookingID = ? AND UPPER([Status]) = 'PENDING'
                """, voucherId, discountAmount, discountAmount, bookingId);
    }

    private static LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}

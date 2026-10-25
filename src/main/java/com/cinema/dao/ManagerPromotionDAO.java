package com.cinema.dao;

import com.cinema.dto.PromotionForm;
import com.cinema.model.ManagerPromotionRow;
import com.cinema.model.ManagerPromotionStats;
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
 * DAO for Manager Promotion campaigns (M11 / UC33 Promotion &amp; Voucher Management).
 * Assignee: Nguyễn Đức Anh (Iter3)
 */
@Repository
public class ManagerPromotionDAO {

    /** Bookings that hold or used a voucher (same rule as the voucher usage limits). */
    private static final String USED = "UPPER(b.[Status]) NOT IN ('CANCELLED', 'EXPIRED')";

    private static final String SELECT = """
            SELECT p.PromotionID, p.[Name], p.[Description], p.StartDate, p.EndDate, p.[Status],
                   (SELECT COUNT(*) FROM dbo.Voucher v WHERE v.PromotionID = p.PromotionID) AS VoucherCount,
                   pv.Code AS PrimaryCode, pv.DiscountType AS PrimaryType, pv.DiscountValue AS PrimaryValue,
                   COALESCE(u.UsageCount, 0) AS UsageCount, COALESCE(u.RefCount, 0) AS RefCount,
                   COALESCE(u.Revenue, 0) AS Revenue, COALESCE(u.DiscountTotal, 0) AS DiscountTotal
            FROM dbo.Promotion p
            OUTER APPLY (SELECT TOP 1 v.Code, v.DiscountType, v.DiscountValue
                         FROM dbo.Voucher v WHERE v.PromotionID = p.PromotionID ORDER BY v.VoucherID) pv
            OUTER APPLY (SELECT SUM(CASE WHEN %s THEN 1 ELSE 0 END) AS UsageCount,
                                COUNT(*) AS RefCount,
                                SUM(CASE WHEN UPPER(b.[Status]) = 'CONFIRMED' THEN b.TotalAmount ELSE 0 END) AS Revenue,
                                SUM(CASE WHEN UPPER(b.[Status]) = 'CONFIRMED' THEN b.DiscountAmount ELSE 0 END) AS DiscountTotal
                         FROM dbo.Booking b JOIN dbo.Voucher v ON v.VoucherID = b.VoucherID
                         WHERE v.PromotionID = p.PromotionID) u
            """.formatted(USED);

    private final JdbcTemplate jdbcTemplate;

    public ManagerPromotionDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<ManagerPromotionRow> mapper = (rs, rowNum) -> {
        ManagerPromotionRow r = new ManagerPromotionRow();
        r.setPromotionId(rs.getInt("PromotionID"));
        r.setName(rs.getString("Name"));
        r.setDescription(rs.getString("Description"));
        r.setStartDate(toLocal(rs, "StartDate"));
        r.setEndDate(toLocal(rs, "EndDate"));
        r.setStatus(rs.getString("Status"));
        r.setVoucherCount(rs.getInt("VoucherCount"));
        r.setPrimaryCode(rs.getString("PrimaryCode"));
        r.setPrimaryDiscountType(rs.getString("PrimaryType"));
        r.setPrimaryDiscountValue(rs.getBigDecimal("PrimaryValue"));
        r.setUsageCount(rs.getInt("UsageCount"));
        r.setReferenceCount(rs.getInt("RefCount"));
        r.setRevenue(rs.getBigDecimal("Revenue"));
        r.setDiscountTotal(rs.getBigDecimal("DiscountTotal"));
        return r;
    };

    public List<ManagerPromotionRow> findAll() {
        return jdbcTemplate.query(SELECT + " ORDER BY p.StartDate DESC, p.PromotionID DESC", mapper);
    }

    public Optional<ManagerPromotionRow> findById(int promotionId) {
        return jdbcTemplate.query(SELECT + " WHERE p.PromotionID = ?", mapper, promotionId).stream().findFirst();
    }

    public int insert(PromotionForm form) {
        Integer id = jdbcTemplate.queryForObject("""
                INSERT INTO dbo.Promotion ([Name], [Description], StartDate, EndDate, [Status])
                OUTPUT inserted.PromotionID
                VALUES (?, ?, ?, ?, ?)
                """, Integer.class, form.getName(), form.getDescription(),
                Timestamp.valueOf(form.getStartDate()), Timestamp.valueOf(form.getEndDate()), form.getStatus());
        return id != null ? id : 0;
    }

    public int update(int promotionId, PromotionForm form) {
        return jdbcTemplate.update("""
                UPDATE dbo.Promotion
                SET [Name] = ?, [Description] = ?, StartDate = ?, EndDate = ?, [Status] = ?
                WHERE PromotionID = ?
                """, form.getName(), form.getDescription(), Timestamp.valueOf(form.getStartDate()),
                Timestamp.valueOf(form.getEndDate()), form.getStatus(), promotionId);
    }

    public int updateStatus(int promotionId, String status) {
        return jdbcTemplate.update("UPDATE dbo.Promotion SET [Status] = ? WHERE PromotionID = ?", status, promotionId);
    }

    /**
     * Hard delete – only when no Booking ever referenced any of its vouchers (M11.4 audit rule).
     * Returns 1 when the promotion was deleted.
     */
    public int deleteIfUnused(int promotionId) {
        String unused = """
                NOT EXISTS (SELECT 1 FROM dbo.Booking b JOIN dbo.Voucher v ON v.VoucherID = b.VoucherID
                            WHERE v.PromotionID = ?)
                """;
        jdbcTemplate.update("DELETE FROM dbo.Voucher WHERE PromotionID = ? AND " + unused, promotionId, promotionId);
        return jdbcTemplate.update("DELETE FROM dbo.Promotion WHERE PromotionID = ? AND " + unused, promotionId, promotionId);
    }

    public ManagerPromotionStats stats() {
        return jdbcTemplate.queryForObject("""
                SELECT
                  (SELECT COUNT(*) FROM dbo.Promotion WHERE UPPER([Status]) = 'ACTIVE'
                       AND StartDate <= SYSDATETIME() AND EndDate >= SYSDATETIME()) AS Running,
                  (SELECT COUNT(*) FROM dbo.Promotion) AS TotalPromotions,
                  (SELECT COUNT(*) FROM dbo.Voucher v JOIN dbo.Promotion p ON p.PromotionID = v.PromotionID
                    WHERE UPPER(v.[Status]) = 'ACTIVE' AND UPPER(p.[Status]) = 'ACTIVE'
                      AND v.ValidFrom <= SYSDATETIME() AND v.ValidTo >= SYSDATETIME()
                      AND p.StartDate <= SYSDATETIME() AND p.EndDate >= SYSDATETIME()) AS LiveVouchers,
                  (SELECT COUNT(*) FROM dbo.Voucher) AS TotalVouchers,
                  (SELECT COUNT(*) FROM dbo.Booking b WHERE b.VoucherID IS NOT NULL AND %s) AS VoucherUses,
                  (SELECT COALESCE(SUM(b.TotalAmount), 0) FROM dbo.Booking b
                    WHERE b.VoucherID IS NOT NULL AND UPPER(b.[Status]) = 'CONFIRMED'
                      AND b.BookingTime >= DATEFROMPARTS(YEAR(SYSDATETIME()), MONTH(SYSDATETIME()), 1)) AS RevenueThisMonth,
                  (SELECT COALESCE(SUM(b.TotalAmount), 0) FROM dbo.Booking b
                    WHERE b.VoucherID IS NOT NULL AND UPPER(b.[Status]) = 'CONFIRMED'
                      AND b.BookingTime >= DATEADD(MONTH, -1, DATEFROMPARTS(YEAR(SYSDATETIME()), MONTH(SYSDATETIME()), 1))
                      AND b.BookingTime < DATEFROMPARTS(YEAR(SYSDATETIME()), MONTH(SYSDATETIME()), 1)) AS RevenueLastMonth,
                  (SELECT COALESCE(SUM(b.DiscountAmount), 0) FROM dbo.Booking b
                    WHERE b.VoucherID IS NOT NULL AND UPPER(b.[Status]) = 'CONFIRMED'
                      AND b.BookingTime >= DATEFROMPARTS(YEAR(SYSDATETIME()), MONTH(SYSDATETIME()), 1)) AS DiscountThisMonth,
                  (SELECT COUNT(*) FROM dbo.Promotion WHERE UPPER([Status]) = 'ACTIVE'
                       AND EndDate >= SYSDATETIME() AND EndDate < DATEADD(DAY, 7, SYSDATETIME())) AS EndingSoon
                """.formatted(USED), (rs, rowNum) -> new ManagerPromotionStats(
                rs.getInt("Running"), rs.getInt("TotalPromotions"),
                rs.getInt("LiveVouchers"), rs.getInt("TotalVouchers"), rs.getInt("VoucherUses"),
                nz(rs.getBigDecimal("RevenueThisMonth")), nz(rs.getBigDecimal("RevenueLastMonth")),
                nz(rs.getBigDecimal("DiscountThisMonth")),
                rs.getInt("EndingSoon"), findEndingSoonNames()));
    }

    private List<String> findEndingSoonNames() {
        return jdbcTemplate.query("""
                SELECT TOP (2) [Name] FROM dbo.Promotion
                WHERE UPPER([Status]) = 'ACTIVE' AND EndDate >= SYSDATETIME() AND EndDate < DATEADD(DAY, 7, SYSDATETIME())
                ORDER BY EndDate
                """, (rs, rowNum) -> rs.getString("Name"));
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static LocalDateTime toLocal(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }
}

package com.cinema.dao;

import com.cinema.dto.VoucherForm;
import com.cinema.model.ManagerVoucherRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO for Manager Vouchers (M11 / UC33 Promotion &amp; Voucher Management).
 * Assignee: Nguyễn Đức Anh (Iter3)
 */
@Repository
public class ManagerVoucherDAO {

    private static final String SELECT = """
            SELECT v.VoucherID, v.PromotionID, p.[Name] AS PromotionName, p.[Status] AS PromotionStatus,
                   p.StartDate AS PromotionStart, p.EndDate AS PromotionEnd,
                   v.Code, v.DiscountType, v.DiscountValue, v.MaxDiscountAmount, v.MinBookingAmount,
                   v.ValidFrom, v.ValidTo, v.TotalUsageLimit, v.PerAccountLimit, v.RequiredRankID,
                   COALESCE(r.DisplayName, r.[Name]) AS RankName, v.[Status],
                   COALESCE(u.UsageCount, 0) AS UsageCount, COALESCE(u.RefCount, 0) AS RefCount,
                   COALESCE(u.Revenue, 0) AS Revenue, COALESCE(u.DiscountTotal, 0) AS DiscountTotal
            FROM dbo.Voucher v
            JOIN dbo.Promotion p ON p.PromotionID = v.PromotionID
            LEFT JOIN dbo.MembershipRank r ON r.RankID = v.RequiredRankID
            OUTER APPLY (SELECT SUM(CASE WHEN UPPER(b.[Status]) NOT IN ('CANCELLED', 'EXPIRED') THEN 1 ELSE 0 END) AS UsageCount,
                                COUNT(*) AS RefCount,
                                SUM(CASE WHEN UPPER(b.[Status]) = 'CONFIRMED' THEN b.TotalAmount ELSE 0 END) AS Revenue,
                                SUM(CASE WHEN UPPER(b.[Status]) = 'CONFIRMED' THEN b.DiscountAmount ELSE 0 END) AS DiscountTotal
                         FROM dbo.Booking b WHERE b.VoucherID = v.VoucherID) u
            """;

    private final JdbcTemplate jdbcTemplate;

    public ManagerVoucherDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<ManagerVoucherRow> mapper = (rs, rowNum) -> {
        ManagerVoucherRow r = new ManagerVoucherRow();
        r.setVoucherId(rs.getInt("VoucherID"));
        r.setPromotionId(rs.getInt("PromotionID"));
        r.setPromotionName(rs.getString("PromotionName"));
        r.setPromotionStatus(rs.getString("PromotionStatus"));
        r.setPromotionStart(toLocal(rs, "PromotionStart"));
        r.setPromotionEnd(toLocal(rs, "PromotionEnd"));
        r.setCode(rs.getString("Code"));
        r.setDiscountType(rs.getString("DiscountType"));
        r.setDiscountValue(rs.getBigDecimal("DiscountValue"));
        r.setMaxDiscountAmount(rs.getBigDecimal("MaxDiscountAmount"));
        r.setMinBookingAmount(rs.getBigDecimal("MinBookingAmount"));
        r.setValidFrom(toLocal(rs, "ValidFrom"));
        r.setValidTo(toLocal(rs, "ValidTo"));
        r.setTotalUsageLimit(nullableInt(rs, "TotalUsageLimit"));
        r.setPerAccountLimit(nullableInt(rs, "PerAccountLimit"));
        r.setRequiredRankId(nullableInt(rs, "RequiredRankID"));
        r.setRequiredRankName(rs.getString("RankName"));
        r.setStatus(rs.getString("Status"));
        r.setUsageCount(rs.getInt("UsageCount"));
        r.setReferenceCount(rs.getInt("RefCount"));
        r.setRevenue(rs.getBigDecimal("Revenue"));
        r.setDiscountTotal(rs.getBigDecimal("DiscountTotal"));
        return r;
    };

    public List<ManagerVoucherRow> findAll() {
        return jdbcTemplate.query(SELECT + " ORDER BY v.ValidFrom DESC, v.VoucherID DESC", mapper);
    }

    public List<ManagerVoucherRow> findByPromotion(int promotionId) {
        return jdbcTemplate.query(SELECT + " WHERE v.PromotionID = ? ORDER BY v.VoucherID", mapper, promotionId);
    }

    public Optional<ManagerVoucherRow> findById(int voucherId) {
        return jdbcTemplate.query(SELECT + " WHERE v.VoucherID = ?", mapper, voucherId).stream().findFirst();
    }

    public boolean codeExists(String code, Integer excludeVoucherId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM dbo.Voucher WHERE UPPER(Code) = UPPER(?) AND VoucherID <> ?
                """, Integer.class, code, excludeVoucherId != null ? excludeVoucherId : 0);
        return count != null && count > 0;
    }

    public int insert(VoucherForm f) {
        Integer id = jdbcTemplate.queryForObject("""
                INSERT INTO dbo.Voucher (PromotionID, RequiredRankID, Code, DiscountType, DiscountValue,
                                         MaxDiscountAmount, MinBookingAmount, ValidFrom, ValidTo,
                                         TotalUsageLimit, PerAccountLimit, [Status])
                OUTPUT inserted.VoucherID
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, Integer.class, f.getPromotionId(), f.getRequiredRankId(), f.getCode(), f.getDiscountType(),
                f.getDiscountValue(), f.getMaxDiscountAmount(), f.getMinBookingAmount(),
                Timestamp.valueOf(f.getValidFrom()), Timestamp.valueOf(f.getValidTo()),
                f.getTotalUsageLimit(), f.getPerAccountLimit(), f.getStatus());
        return id != null ? id : 0;
    }

    public int update(int voucherId, VoucherForm f) {
        return jdbcTemplate.update("""
                UPDATE dbo.Voucher
                SET PromotionID = ?, RequiredRankID = ?, Code = ?, DiscountType = ?, DiscountValue = ?,
                    MaxDiscountAmount = ?, MinBookingAmount = ?, ValidFrom = ?, ValidTo = ?,
                    TotalUsageLimit = ?, PerAccountLimit = ?, [Status] = ?
                WHERE VoucherID = ?
                """, f.getPromotionId(), f.getRequiredRankId(), f.getCode(), f.getDiscountType(),
                f.getDiscountValue(), f.getMaxDiscountAmount(), f.getMinBookingAmount(),
                Timestamp.valueOf(f.getValidFrom()), Timestamp.valueOf(f.getValidTo()),
                f.getTotalUsageLimit(), f.getPerAccountLimit(), f.getStatus(), voucherId);
    }

    public int updateStatus(int voucherId, String status) {
        return jdbcTemplate.update("UPDATE dbo.Voucher SET [Status] = ? WHERE VoucherID = ?", status, voucherId);
    }

    /** Hard delete – only when no Booking ever referenced the voucher (M11.4 audit rule). */
    public int deleteIfUnused(int voucherId) {
        return jdbcTemplate.update("""
                DELETE FROM dbo.Voucher
                WHERE VoucherID = ? AND NOT EXISTS (SELECT 1 FROM dbo.Booking b WHERE b.VoucherID = ?)
                """, voucherId, voucherId);
    }

    private static LocalDateTime toLocal(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}

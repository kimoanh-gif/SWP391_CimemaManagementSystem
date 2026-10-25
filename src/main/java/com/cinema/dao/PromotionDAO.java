package com.cinema.dao;

import com.cinema.model.Promotion;
import com.cinema.model.Voucher;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * DAO for public Promotion data (UC04 – View Promotions &amp; News).
 *
 * Business rules applied in SQL:
 *  BR-PN-01  only ACTIVE promotions are public
 *  BR-PN-02  StartDate &lt;= now &lt;= EndDate (server time zone)
 *  BR-PN-05  inactive / expired / upcoming promotions are never returned
 */
@Repository
public class PromotionDAO {

    private static final String PROMOTION_COLUMNS = """
            SELECT p.PromotionID, p.[Name], p.[Description], p.StartDate, p.EndDate, p.[Status]
            FROM dbo.Promotion p
            """;

    private static final String PUBLIC_PROMOTION_FILTER = """
            WHERE UPPER(p.[Status]) = 'ACTIVE'
              AND p.StartDate <= SYSDATETIME()
              AND p.EndDate >= SYSDATETIME()
            """;

    private final JdbcTemplate jdbcTemplate;

    public PromotionDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Promotion> promoMapper = (rs, rowNum) -> mapPromotion(rs);
    private final RowMapper<Voucher> voucherMapper = (rs, rowNum) -> mapVoucher(rs);

    private Promotion mapPromotion(ResultSet rs) throws SQLException {
        Promotion p = new Promotion();
        p.setPromotionId(rs.getInt("PromotionID"));
        p.setName(rs.getString("Name"));
        p.setDescription(rs.getString("Description"));

        Timestamp start = rs.getTimestamp("StartDate");
        p.setStartDate(start != null ? start.toLocalDateTime() : null);

        Timestamp end = rs.getTimestamp("EndDate");
        p.setEndDate(end != null ? end.toLocalDateTime() : null);

        p.setStatus(rs.getString("Status"));
        return p;
    }

    private Voucher mapVoucher(ResultSet rs) throws SQLException {
        Voucher v = new Voucher();
        v.setVoucherId(rs.getInt("VoucherID"));
        v.setPromotionId(rs.getInt("PromotionID"));
        int rankId = rs.getInt("RequiredRankID");
        if (!rs.wasNull()) v.setRequiredRankId(rankId);
        v.setCode(rs.getString("Code"));
        v.setDiscountType(rs.getString("DiscountType"));
        v.setDiscountValue(rs.getBigDecimal("DiscountValue"));
        v.setMaxDiscountAmount(rs.getBigDecimal("MaxDiscountAmount"));
        v.setMinBookingAmount(rs.getBigDecimal("MinBookingAmount"));

        Timestamp vf = rs.getTimestamp("ValidFrom");
        v.setValidFrom(vf != null ? vf.toLocalDateTime() : null);

        Timestamp vt = rs.getTimestamp("ValidTo");
        v.setValidTo(vt != null ? vt.toLocalDateTime() : null);

        int totalLimit = rs.getInt("TotalUsageLimit");
        if (!rs.wasNull()) v.setTotalUsageLimit(totalLimit);
        int perAccount = rs.getInt("PerAccountLimit");
        if (!rs.wasNull()) v.setPerAccountLimit(perAccount);

        v.setStatus(rs.getString("Status"));
        return v;
    }

    /** All promotions that are currently public, newest first. */
    public List<Promotion> findActivePromotions() {
        String sql = PROMOTION_COLUMNS + PUBLIC_PROMOTION_FILTER + " ORDER BY p.StartDate DESC";
        return attachVouchers(jdbcTemplate.query(sql, promoMapper));
    }

    /** The newest public promotions (used by the Home Page). */
    public List<Promotion> findLatestPromotions(int limit) {
        if (limit <= 0) {
            return findActivePromotions();
        }
        String sql = """
                SELECT TOP (?) p.PromotionID, p.[Name], p.[Description], p.StartDate, p.EndDate, p.[Status]
                FROM dbo.Promotion p
                """ + PUBLIC_PROMOTION_FILTER + " ORDER BY p.StartDate DESC";
        return attachVouchers(jdbcTemplate.query(sql, promoMapper, limit));
    }

    /**
     * A single promotion only if it is currently public.
     * Expired / inactive promotions cannot be opened by typing the ID in the URL (BR-PN-05).
     */
    public Optional<Promotion> findPublicById(int promotionId) {
        String sql = PROMOTION_COLUMNS + PUBLIC_PROMOTION_FILTER + " AND p.PromotionID = ?";
        try {
            Promotion p = jdbcTemplate.queryForObject(sql, promoMapper, promotionId);
            if (p != null) {
                p.setVouchers(findVouchersByPromotionId(promotionId));
            }
            return Optional.ofNullable(p);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /** Any promotion by ID regardless of status (kept for back-office use). */
    public Optional<Promotion> findById(int promotionId) {
        String sql = PROMOTION_COLUMNS + " WHERE p.PromotionID = ?";
        try {
            Promotion p = jdbcTemplate.queryForObject(sql, promoMapper, promotionId);
            if (p != null) {
                p.setVouchers(findVouchersByPromotionId(promotionId));
            }
            return Optional.ofNullable(p);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /** Active vouchers of a promotion that are still inside their validity window. */
    public List<Voucher> findVouchersByPromotionId(int promotionId) {
        String sql = """
                SELECT v.VoucherID, v.PromotionID, v.RequiredRankID, v.Code,
                       v.DiscountType, v.DiscountValue, v.MaxDiscountAmount, v.MinBookingAmount,
                       v.ValidFrom, v.ValidTo, v.TotalUsageLimit, v.PerAccountLimit, v.[Status]
                FROM dbo.Voucher v
                WHERE v.PromotionID = ?
                  AND UPPER(v.[Status]) = 'ACTIVE'
                  AND v.ValidTo >= SYSDATETIME()
                ORDER BY v.DiscountValue DESC
                """;
        return jdbcTemplate.query(sql, voucherMapper, promotionId);
    }

    private List<Promotion> attachVouchers(List<Promotion> promotions) {
        for (Promotion p : promotions) {
            p.setVouchers(findVouchersByPromotionId(p.getPromotionId()));
        }
        return promotions;
    }
}

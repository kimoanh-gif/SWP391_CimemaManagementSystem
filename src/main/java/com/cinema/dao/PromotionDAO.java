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

@Repository
public class PromotionDAO {

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

        v.setStatus(rs.getString("Status"));
        return v;
    }

    public List<Promotion> findActivePromotions() {
        String sql = """
                SELECT p.PromotionID, p.[Name], p.[Description], p.StartDate, p.EndDate, p.[Status]
                FROM dbo.Promotion p
                WHERE UPPER(p.[Status]) = 'ACTIVE'
                  AND p.StartDate <= SYSDATETIME()
                  AND (p.EndDate IS NULL OR p.EndDate >= SYSDATETIME())
                ORDER BY p.StartDate DESC
                """;
        List<Promotion> promotions = jdbcTemplate.query(sql, promoMapper);
        for (Promotion p : promotions) {
            p.setVouchers(findVouchersByPromotionId(p.getPromotionId()));
        }
        return promotions;
    }

    public List<Promotion> findLatestPromotions(int limit) {
        String sql = """
                SELECT p.PromotionID, p.[Name], p.[Description], p.StartDate, p.EndDate, p.[Status]
                FROM dbo.Promotion p
                WHERE UPPER(p.[Status]) = 'ACTIVE'
                  AND p.StartDate <= SYSDATETIME()
                  AND (p.EndDate IS NULL OR p.EndDate >= SYSDATETIME())
                ORDER BY p.StartDate DESC
                """;
        if (limit > 0) {
            sql = sql.replaceFirst("SELECT ", "SELECT TOP (" + limit + ") ");
        }
        List<Promotion> promotions = jdbcTemplate.query(sql, promoMapper);
        for (Promotion p : promotions) {
            p.setVouchers(findVouchersByPromotionId(p.getPromotionId()));
        }
        return promotions;
    }

    public Optional<Promotion> findById(int promotionId) {
        String sql = """
                SELECT p.PromotionID, p.[Name], p.[Description], p.StartDate, p.EndDate, p.[Status]
                FROM dbo.Promotion p
                WHERE p.PromotionID = ?
                """;
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

    /**
     * Finds a promotion only when it can be shown publicly:
     * ACTIVE and the current time is inside [StartDate, EndDate] (BR-PN-01, BR-PN-02, BR-PN-05).
     */
    public Optional<Promotion> findPublicById(int promotionId) {
        String sql = """
                SELECT p.PromotionID, p.[Name], p.[Description], p.StartDate, p.EndDate, p.[Status]
                FROM dbo.Promotion p
                WHERE p.PromotionID = ?
                  AND UPPER(p.[Status]) = 'ACTIVE'
                  AND p.StartDate <= SYSDATETIME()
                  AND (p.EndDate IS NULL OR p.EndDate >= SYSDATETIME())
                """;
        List<Promotion> result = jdbcTemplate.query(sql, promoMapper, promotionId);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        Promotion p = result.get(0);
        p.setVouchers(findVouchersByPromotionId(promotionId));
        return Optional.of(p);
    }

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
}

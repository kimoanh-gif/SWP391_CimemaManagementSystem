package com.cinema.dao;

import com.cinema.model.TicketPricing;
import com.cinema.model.TicketPricingForm;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for TicketPricing table.
 * Supports Screen 60 (Ticket Pricing Management) and ticket price calculation per BR-50/BR-51.
 */
@Repository
public class TicketPricingDao {

    private final JdbcTemplate jdbcTemplate;

    public TicketPricingDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<TicketPricing> rowMapper = (rs, rowNum) -> {
        TicketPricing tp = new TicketPricing();
        tp.setPricingId(rs.getInt("PricingID"));
        tp.setPricingCode(rs.getString("PricingCode"));
        tp.setRuleName(rs.getString("RuleName"));
        tp.setDayType(rs.getString("DayType"));
        tp.setTimeSlot(rs.getString("TimeSlot"));
        tp.setBasePrice(rs.getBigDecimal("BasePrice"));
        tp.setFormatSurcharge(rs.getBigDecimal("FormatSurcharge"));
        tp.setExperienceType(rs.getString("ExperienceType"));
        tp.setStatus(rs.getString("Status"));
        tp.setDescription(rs.getString("Description"));
        tp.setCreatedAt(rs.getTimestamp("CreatedAt") != null ? rs.getTimestamp("CreatedAt").toLocalDateTime() : null);
        tp.setUpdatedAt(rs.getTimestamp("UpdatedAt") != null ? rs.getTimestamp("UpdatedAt").toLocalDateTime() : null);
        return tp;
    };

    public List<TicketPricing> findAll(String dayType, String status) {
        String sql = "SELECT * FROM TicketPricing WHERE " +
                "(? IS NULL OR DayType = ?) AND " +
                "(? IS NULL OR Status = ?) " +
                "ORDER BY BasePrice ASC, PricingID ASC";
        String dt = (dayType == null || "ALL".equalsIgnoreCase(dayType)) ? null : dayType;
        String st = (status == null || "ALL".equalsIgnoreCase(status)) ? null : status;
        return jdbcTemplate.query(sql, ps -> {
            ps.setString(1, dt);
            ps.setString(2, dt);
            ps.setString(3, st);
            ps.setString(4, st);
        }, rowMapper);
    }

    public Optional<TicketPricing> findById(int id) {
        String sql = "SELECT * FROM TicketPricing WHERE PricingID = ?";
        List<TicketPricing> list = jdbcTemplate.query(sql, ps -> ps.setInt(1, id), rowMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<TicketPricing> findByCode(String code) {
        String sql = "SELECT * FROM TicketPricing WHERE PricingCode = ?";
        List<TicketPricing> list = jdbcTemplate.query(sql, ps -> ps.setString(1, code), rowMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public int insert(TicketPricingForm form) {
        String sql = "INSERT INTO TicketPricing (PricingCode, RuleName, DayType, TimeSlot, BasePrice, FormatSurcharge, ExperienceType, Status, Description, CreatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, SYSUTCDATETIME())";
        return jdbcTemplate.update(sql,
                form.getPricingCode().trim(),
                form.getRuleName().trim(),
                form.getDayType(),
                form.getTimeSlot(),
                form.getBasePrice(),
                form.getFormatSurcharge() != null ? form.getFormatSurcharge() : BigDecimal.ZERO,
                form.getExperienceType() != null ? form.getExperienceType() : "2D Digital",
                form.getStatus() != null ? form.getStatus() : "Active",
                form.getDescription()
        );
    }

    public int update(int id, TicketPricingForm form) {
        String sql = "UPDATE TicketPricing SET PricingCode = ?, RuleName = ?, DayType = ?, TimeSlot = ?, " +
                "BasePrice = ?, FormatSurcharge = ?, ExperienceType = ?, Status = ?, Description = ?, " +
                "UpdatedAt = SYSUTCDATETIME() WHERE PricingID = ?";
        return jdbcTemplate.update(sql,
                form.getPricingCode().trim(),
                form.getRuleName().trim(),
                form.getDayType(),
                form.getTimeSlot(),
                form.getBasePrice(),
                form.getFormatSurcharge() != null ? form.getFormatSurcharge() : BigDecimal.ZERO,
                form.getExperienceType(),
                form.getStatus(),
                form.getDescription(),
                id
        );
    }

    public int updateBasePrice(int id, BigDecimal basePrice) {
        String sql = "UPDATE TicketPricing SET BasePrice = ?, UpdatedAt = SYSUTCDATETIME() WHERE PricingID = ?";
        return jdbcTemplate.update(sql, basePrice, id);
    }

    public int updateStatus(int id, String status) {
        String sql = "UPDATE TicketPricing SET Status = ?, UpdatedAt = SYSUTCDATETIME() WHERE PricingID = ?";
        return jdbcTemplate.update(sql, status, id);
    }

    public BigDecimal getAverageTicketPrice() {
        String sql = "SELECT AVG(BasePrice + FormatSurcharge) FROM TicketPricing WHERE Status = 'Active'";
        BigDecimal avg = jdbcTemplate.queryForObject(sql, BigDecimal.class);
        return avg != null ? avg : BigDecimal.ZERO;
    }
}

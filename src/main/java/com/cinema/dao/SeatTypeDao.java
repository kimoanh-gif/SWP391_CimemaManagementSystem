package com.cinema.dao;

import com.cinema.model.SeatType;
import com.cinema.dto.SeatTypeForm;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for SeatType table.
 * Supports Screen 57 (Seat Type Management) and Screen 60 (Ticket Pricing Management).
 */
@Repository
public class SeatTypeDao {

    private final JdbcTemplate jdbcTemplate;

    public SeatTypeDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<SeatType> rowMapper = (rs, rowNum) -> {
        SeatType st = new SeatType();
        st.setSeatTypeId(rs.getInt("SeatTypeID"));
        st.setTypeCode(rs.getString("TypeCode"));
        st.setTypeName(rs.getString("TypeName"));
        st.setDescription(rs.getString("Description"));
        st.setPriceMultiplier(rs.getBigDecimal("PriceMultiplier"));
        st.setColorCode(rs.getString("ColorCode"));
        st.setStatus(rs.getString("Status"));
        st.setCreatedAt(rs.getTimestamp("CreatedAt") != null ? rs.getTimestamp("CreatedAt").toLocalDateTime() : null);
        st.setUpdatedAt(rs.getTimestamp("UpdatedAt") != null ? rs.getTimestamp("UpdatedAt").toLocalDateTime() : null);
        return st;
    };

    public List<SeatType> findAll(String status) {
        String sql = "SELECT * FROM SeatType WHERE (? IS NULL OR Status = ?) ORDER BY PriceMultiplier ASC";
        String param = (status == null || "ALL".equalsIgnoreCase(status)) ? null : status;
        return jdbcTemplate.query(sql, ps -> {
            ps.setString(1, param);
            ps.setString(2, param);
        }, rowMapper);
    }

    public Optional<SeatType> findById(int id) {
        String sql = "SELECT * FROM SeatType WHERE SeatTypeID = ?";
        List<SeatType> list = jdbcTemplate.query(sql, ps -> ps.setInt(1, id), rowMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<SeatType> findByCode(String code) {
        String sql = "SELECT * FROM SeatType WHERE TypeCode = ?";
        List<SeatType> list = jdbcTemplate.query(sql, ps -> ps.setString(1, code), rowMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<SeatType> findByName(String name) {
        String sql = "SELECT * FROM SeatType WHERE TypeName = ?";
        List<SeatType> list = jdbcTemplate.query(sql, ps -> ps.setString(1, name), rowMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public int insert(SeatTypeForm form) {
        String sql = "INSERT INTO SeatType (TypeCode, TypeName, Description, PriceMultiplier, ColorCode, Status, CreatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, SYSUTCDATETIME())";
        return jdbcTemplate.update(sql,
                form.getTypeCode().trim(),
                form.getTypeName().trim(),
                form.getDescription(),
                form.getPriceMultiplier(),
                form.getColorCode() != null ? form.getColorCode() : "#3B82F6",
                form.getStatus() != null ? form.getStatus() : "Active"
        );
    }

    public int update(int id, SeatTypeForm form) {
        String sql = "UPDATE SeatType SET TypeCode = ?, TypeName = ?, Description = ?, " +
                "PriceMultiplier = ?, ColorCode = ?, Status = ?, UpdatedAt = SYSUTCDATETIME() " +
                "WHERE SeatTypeID = ?";
        return jdbcTemplate.update(sql,
                form.getTypeCode().trim(),
                form.getTypeName().trim(),
                form.getDescription(),
                form.getPriceMultiplier(),
                form.getColorCode(),
                form.getStatus(),
                id
        );
    }

    public int updateMultiplier(int id, BigDecimal multiplier) {
        String sql = "UPDATE SeatType SET PriceMultiplier = ?, UpdatedAt = SYSUTCDATETIME() WHERE SeatTypeID = ?";
        return jdbcTemplate.update(sql, multiplier, id);
    }

    public int updateStatus(int id, String status) {
        String sql = "UPDATE SeatType SET Status = ?, UpdatedAt = SYSUTCDATETIME() WHERE SeatTypeID = ?";
        return jdbcTemplate.update(sql, status, id);
    }

    public boolean isReferencedBySeats(int id) {
        String sql = "SELECT COUNT(*) FROM Seat s " +
                "WHERE s.SeatTypeID = ? " +
                "OR (s.SeatTypeID IS NULL AND (s.SeatTypeName = (SELECT TypeName FROM SeatType WHERE SeatTypeID = ?) " +
                "OR s.SeatTypeName = (SELECT TypeCode FROM SeatType WHERE SeatTypeID = ?)))";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id, id, id);
        return count != null && count > 0;
    }

    public int delete(int id) {
        String sql = "DELETE FROM SeatType WHERE SeatTypeID = ?";
        return jdbcTemplate.update(sql, id);
    }
}

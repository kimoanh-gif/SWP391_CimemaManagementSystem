package com.cinema.dao;

import com.cinema.model.LoginAttempt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Repository
public class LoginAttemptDAO {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<LoginAttempt> rowMapper = (rs, rowNum) -> mapResultSetToModel(rs);

    @Autowired
    public LoginAttemptDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private LoginAttempt mapResultSetToModel(ResultSet rs) throws SQLException {
        LoginAttempt a = new LoginAttempt();
        a.setAttemptId     (rs.getLong("AttemptID"));
        a.setIdentifier    (rs.getString("Identifier"));
        a.setIpAddress     (rs.getString("IPAddress"));
        a.setUserAgent     (rs.getString("UserAgent"));
        a.setSuccess       (rs.getBoolean("Success"));
        a.setFailureReason (rs.getString("FailureReason"));
        a.setAuthMethod    (rs.getString("AuthMethod"));
        Timestamp at = rs.getTimestamp("AttemptedAt");
        if (at != null) a.setAttemptedAt(at.toLocalDateTime());
        return a;
    }

    public Long insert(LoginAttempt a) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO dbo.LoginAttempt (Identifier, IPAddress, UserAgent, Success, FailureReason, AuthMethod)
            OUTPUT inserted.AttemptID
            VALUES (?, ?, ?, ?, ?, ?)
            """, Long.class,
                a.getIdentifier(),
                a.getIpAddress(),
                a.getUserAgent(),
                a.isSuccess(),
                a.getFailureReason(),
                a.getAuthMethod());
    }

    public int countRecentFailuresByIdentifier(String identifier, int windowMinutes) {
        Integer n = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM dbo.LoginAttempt
            WHERE LOWER(LTRIM(RTRIM(Identifier))) = LOWER(LTRIM(RTRIM(?)))
              AND Success = 0
              AND AttemptedAt > DATEADD(MINUTE, -?, SYSDATETIME())
            """, Integer.class, identifier, windowMinutes);
        return n == null ? 0 : n;
    }

    public int countRecentFailuresByIp(String ipAddress, int windowMinutes) {
        if (ipAddress == null || ipAddress.isBlank()) return 0;
        Integer n = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM dbo.LoginAttempt
            WHERE IPAddress = ?
              AND Success = 0
              AND AttemptedAt > DATEADD(MINUTE, -?, SYSDATETIME())
            """, Integer.class, ipAddress, windowMinutes);
        return n == null ? 0 : n;
    }

    public LocalDateTime findLastFailureTime(String identifier) {
        try {
            Timestamp ts = jdbcTemplate.queryForObject("""
                SELECT TOP 1 AttemptedAt FROM dbo.LoginAttempt
                WHERE LOWER(LTRIM(RTRIM(Identifier))) = LOWER(LTRIM(RTRIM(?)))
                  AND Success = 0
                ORDER BY AttemptedAt DESC
                """, Timestamp.class, identifier);
            return ts == null ? null : ts.toLocalDateTime();
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public int deleteOlderThanDays(int days) {
        return jdbcTemplate.update(
                "DELETE FROM dbo.LoginAttempt WHERE AttemptedAt < DATEADD(DAY, -?, SYSDATETIME())",
                days);
    }
}

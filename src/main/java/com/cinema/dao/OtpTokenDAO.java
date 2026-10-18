package com.cinema.dao;

import com.cinema.model.OtpToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class OtpTokenDAO {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<OtpToken> otpRowMapper = (rs, rowNum) -> mapResultSetToModel(rs);

    @Autowired
    public OtpTokenDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private OtpToken mapResultSetToModel(ResultSet rs) throws SQLException {
        OtpToken t = new OtpToken();
        t.setOtpTokenId   (rs.getLong("OtpTokenID"));
        t.setAccountId    ((Integer) rs.getObject("AccountID"));
        t.setEmail        (rs.getString("Email"));
        t.setCodeHash     (rs.getString("CodeHash"));
        t.setPurpose      (rs.getString("Purpose"));
        Timestamp exp = rs.getTimestamp("ExpiresAt");
        if (exp != null) t.setExpiresAt(exp.toLocalDateTime());
        Timestamp cons = rs.getTimestamp("ConsumedAt");
        if (cons != null) t.setConsumedAt(cons.toLocalDateTime());
        t.setAttemptCount (rs.getInt("AttemptCount"));
        Timestamp crt = rs.getTimestamp("CreatedAt");
        if (crt != null) t.setCreatedAt(crt.toLocalDateTime());
        Timestamp sent = rs.getTimestamp("LastSentAt");
        if (sent != null) t.setLastSentAt(sent.toLocalDateTime());
        return t;
    }

    public Long insert(OtpToken t) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO dbo.OtpToken (AccountID, Email, CodeHash, Purpose, ExpiresAt, LastSentAt)
            OUTPUT inserted.OtpTokenID
            VALUES (?, ?, ?, ?, ?, ?)
            """, Long.class,
                t.getAccountId(),
                t.getEmail(),
                t.getCodeHash(),
                t.getPurpose(),
                Timestamp.valueOf(t.getExpiresAt()),
                Timestamp.valueOf(t.getLastSentAt() == null ? LocalDateTime.now() : t.getLastSentAt()));
    }

    /**
     * Returns the latest unconsumed token for the email + purpose that has not yet expired.
     */
    public Optional<OtpToken> findActiveByEmailAndPurpose(String email, String purpose) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                SELECT TOP 1 OtpTokenID, AccountID, Email, CodeHash, Purpose,
                       ExpiresAt, ConsumedAt, AttemptCount, CreatedAt, LastSentAt
                FROM dbo.OtpToken
                WHERE LOWER(LTRIM(RTRIM(Email))) = LOWER(LTRIM(RTRIM(?)))
                  AND Purpose = ?
                  AND ConsumedAt IS NULL
                  AND ExpiresAt > SYSDATETIME()
                ORDER BY CreatedAt DESC
                """, otpRowMapper, email, purpose));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /** Returns the most recent token for the (email, purpose) regardless of consumed/expired state. */
    public Optional<OtpToken> findLatestByEmailAndPurpose(String email, String purpose) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                SELECT TOP 1 OtpTokenID, AccountID, Email, CodeHash, Purpose,
                       ExpiresAt, ConsumedAt, AttemptCount, CreatedAt, LastSentAt
                FROM dbo.OtpToken
                WHERE LOWER(LTRIM(RTRIM(Email))) = LOWER(LTRIM(RTRIM(?)))
                  AND Purpose = ?
                ORDER BY CreatedAt DESC
                """, otpRowMapper, email, purpose));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public int incrementAttempt(long otpTokenId) {
        return jdbcTemplate.update(
                "UPDATE dbo.OtpToken SET AttemptCount = AttemptCount + 1 WHERE OtpTokenID = ?",
                otpTokenId);
    }

    public int markConsumed(long otpTokenId) {
        return jdbcTemplate.update(
                "UPDATE dbo.OtpToken SET ConsumedAt = ? WHERE OtpTokenID = ?",
                Timestamp.valueOf(LocalDateTime.now()));
    }

    public int touchLastSent(long otpTokenId) {
        return jdbcTemplate.update(
                "UPDATE dbo.OtpToken SET LastSentAt = SYSDATETIME() WHERE OtpTokenID = ?",
                otpTokenId);
    }

    public int deleteAllByEmailAndPurpose(String email, String purpose) {
        return jdbcTemplate.update(
                "DELETE FROM dbo.OtpToken WHERE LOWER(LTRIM(RTRIM(Email))) = LOWER(LTRIM(RTRIM(?))) AND Purpose = ?",
                email, purpose);
    }

    public List<OtpToken> findAllByEmailAndPurpose(String email, String purpose) {
        return jdbcTemplate.query("""
            SELECT OtpTokenID, AccountID, Email, CodeHash, Purpose, ExpiresAt, ConsumedAt,
                   AttemptCount, CreatedAt, LastSentAt
            FROM dbo.OtpToken
            WHERE LOWER(LTRIM(RTRIM(Email))) = LOWER(LTRIM(RTRIM(?))) AND Purpose = ?
            ORDER BY CreatedAt DESC
            """, otpRowMapper, email, purpose);
    }
}

package com.cinema.dao;

import com.cinema.model.OAuthAccount;
import org.springframework.beans.factory.annotation.Autowired;
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
public class OAuthAccountDAO {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<OAuthAccount> rowMapper = (rs, rowNum) -> mapResultSetToModel(rs);

    @Autowired
    public OAuthAccountDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private OAuthAccount mapResultSetToModel(ResultSet rs) throws SQLException {
        OAuthAccount o = new OAuthAccount();
        o.setOAuthAccountId        (rs.getLong("OAuthAccountID"));
        o.setAccountId             (rs.getInt("AccountID"));
        o.setProvider              (rs.getString("Provider"));
        o.setProviderSubject       (rs.getString("ProviderSubject"));
        o.setProviderEmail         (rs.getString("ProviderEmail"));
        o.setEmailVerifiedByProvider(rs.getBoolean("EmailVerifiedByProvider"));
        Timestamp linked = rs.getTimestamp("LinkedAt");
        if (linked != null) o.setLinkedAt(linked.toLocalDateTime());
        Timestamp last = rs.getTimestamp("LastLoginAt");
        if (last != null) o.setLastLoginAt(last.toLocalDateTime());
        return o;
    }

    /** Look up by the provider's stable user identifier (the OIDC `sub` claim). */
    public Optional<OAuthAccount> findByProviderSubject(String provider, String subject) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                SELECT OAuthAccountID, AccountID, Provider, ProviderSubject, ProviderEmail,
                       EmailVerifiedByProvider, LinkedAt, LastLoginAt
                FROM dbo.OAuthAccount
                WHERE Provider = ? AND ProviderSubject = ?
                """, rowMapper, provider, subject));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<OAuthAccount> findAllByAccountId(int accountId) {
        return jdbcTemplate.query("""
            SELECT OAuthAccountID, AccountID, Provider, ProviderSubject, ProviderEmail,
                   EmailVerifiedByProvider, LinkedAt, LastLoginAt
            FROM dbo.OAuthAccount
            WHERE AccountID = ?
            ORDER BY LinkedAt DESC
            """, rowMapper, accountId);
    }

    public Long insert(OAuthAccount o) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO dbo.OAuthAccount (AccountID, Provider, ProviderSubject, ProviderEmail, EmailVerifiedByProvider)
            OUTPUT inserted.OAuthAccountID
            VALUES (?, ?, ?, ?, ?)
            """, Long.class,
                o.getAccountId(),
                o.getProvider(),
                o.getProviderSubject(),
                o.getProviderEmail(),
                o.isEmailVerifiedByProvider());
    }

    public int touchLastLogin(long oAuthAccountId) {
        return jdbcTemplate.update(
                "UPDATE dbo.OAuthAccount SET LastLoginAt = SYSDATETIME() WHERE OAuthAccountID = ?",
                oAuthAccountId);
    }

    public int updateEmail(long oAuthAccountId, String newEmail, boolean verified) {
        return jdbcTemplate.update("""
            UPDATE dbo.OAuthAccount
            SET ProviderEmail = ?, EmailVerifiedByProvider = ?
            WHERE OAuthAccountID = ?
            """, newEmail, verified, oAuthAccountId);
    }
}

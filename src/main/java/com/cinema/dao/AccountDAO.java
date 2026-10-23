package com.cinema.dao;

import com.cinema.model.Account;
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

/**
 * Account DAO – Spring JdbcTemplate.
 * All queries are centralised; manual mapping goes through mapResultSetToModel().
 */
@Repository
public class AccountDAO {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Account> accountRowMapper = (rs, rowNum) -> mapResultSetToModel(rs);

    @Autowired
    public AccountDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---------------------------------------------------------- helpers

    /** Single mapping rule for Account – used by every read query. */
    private Account mapResultSetToModel(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setAccountId    (rs.getInt      ("AccountID"));
        a.setRoleId       (rs.getInt      ("RoleID"));
        a.setUsername     (rs.getString   ("Username"));
        a.setEmail        (rs.getString   ("Email"));
        a.setPasswordHash (rs.getString   ("PasswordHash"));
        a.setFullName     (rs.getString   ("FullName"));
        a.setPhone        (rs.getString   ("Phone"));
        a.setStatus       (rs.getString   ("Status"));
        Timestamp verified = rs.getTimestamp("EmailVerifiedAt");
        a.setEmailVerified(verified != null);
        if (verified != null) a.setEmailVerifiedAt(verified.toLocalDateTime());
        Timestamp created = rs.getTimestamp("CreatedAt");
        if (created != null) a.setCreatedAt(created.toLocalDateTime());
        Timestamp lastLogin = rs.getTimestamp("LastLoginAt");
        if (lastLogin != null) a.setLastLoginAt(lastLogin.toLocalDateTime());
        return a;
    }

    // ---------------------------------------------------------- reads

    public Optional<Account> findByEmail(String email) {
        final String sql = """
            SELECT AccountID, RoleID, Username, Email, PasswordHash, FullName, Phone, Status,
                   EmailVerifiedAt, CreatedAt, LastLoginAt
            FROM dbo.Account
            WHERE LOWER(LTRIM(RTRIM(Email))) = ?
            """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, accountRowMapper,
                    email.trim().toLowerCase()));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Optional<Account> findByUsername(String username) {
        final String sql = """
            SELECT AccountID, RoleID, Username, Email, PasswordHash, FullName, Phone, Status,
                   EmailVerifiedAt, CreatedAt, LastLoginAt
            FROM dbo.Account
            WHERE LOWER(LTRIM(RTRIM(Username))) = ?
            """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, accountRowMapper,
                    username.trim().toLowerCase()));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /** Login lookup: tries email first, then username. */
    public Optional<Account> findByEmailOrUsername(String identifier) {
        if (identifier == null) return Optional.empty();
        String id = identifier.trim().toLowerCase();
        if (id.contains("@")) return findByEmail(id);
        return findByUsername(id);
    }

    public Optional<Account> findById(int accountId) {
        final String sql = """
            SELECT AccountID, RoleID, Username, Email, PasswordHash, FullName, Phone, Status,
                   EmailVerifiedAt, CreatedAt, LastLoginAt
            FROM dbo.Account WHERE AccountID = ?
            """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, accountRowMapper, accountId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /** Serializes simultaneous resets until the password update and email attempt finish. */
    public Optional<Account> findByIdForPasswordReset(int accountId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                SELECT AccountID, RoleID, Username, Email, PasswordHash, FullName, Phone, Status,
                       EmailVerifiedAt, CreatedAt, LastLoginAt
                FROM dbo.Account WITH (UPDLOCK, HOLDLOCK) WHERE AccountID = ?
                """, accountRowMapper, accountId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public boolean existsByEmail(String email) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.Account WHERE LOWER(LTRIM(RTRIM(Email))) = LOWER(LTRIM(RTRIM(?)))",
                Integer.class, email);
        return n != null && n > 0;
    }

    public boolean existsByUsername(String username) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.Account WHERE LOWER(LTRIM(RTRIM(Username))) = LOWER(LTRIM(RTRIM(?)))",
                Integer.class, username);
        return n != null && n > 0;
    }

    public boolean existsByPhone(String phone) {
        if (phone == null || phone.isBlank()) return false;
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.Account WHERE LOWER(LTRIM(RTRIM(Phone))) = LOWER(LTRIM(RTRIM(?)))",
                Integer.class, phone.trim());
        return n != null && n > 0;
    }

    public List<Account> findAllByStatus(String status) {
        return jdbcTemplate.query("""
            SELECT AccountID, RoleID, Username, Email, PasswordHash, FullName, Phone, Status,
                   EmailVerifiedAt, CreatedAt, LastLoginAt
            FROM dbo.Account WHERE Status = ? ORDER BY AccountID DESC
            """, accountRowMapper, status);
    }

    public List<Account> findAll() {
        return jdbcTemplate.query("""
            SELECT AccountID, RoleID, Username, Email, PasswordHash, FullName, Phone, Status,
                   EmailVerifiedAt, CreatedAt, LastLoginAt
            FROM dbo.Account ORDER BY AccountID DESC
            """, accountRowMapper);
    }

    public List<Account> findAllByRoleId(int roleId) {
        return jdbcTemplate.query("""
            SELECT AccountID, RoleID, Username, Email, PasswordHash, FullName, Phone, Status,
                   EmailVerifiedAt, CreatedAt, LastLoginAt
            FROM dbo.Account WHERE RoleID = ? ORDER BY AccountID DESC
            """, accountRowMapper, roleId);
    }

    public List<Account> searchByKeyword(String keyword) {
        String sql = """
            SELECT AccountID, RoleID, Username, Email, PasswordHash, FullName, Phone, Status,
                   EmailVerifiedAt, CreatedAt, LastLoginAt
            FROM dbo.Account
            WHERE LOWER(Email) LIKE ? OR LOWER(Username) LIKE ? OR LOWER(FullName) LIKE ?
            ORDER BY AccountID DESC
            """;
        String pattern = "%" + keyword.toLowerCase() + "%";
        return jdbcTemplate.query(sql, accountRowMapper, pattern, pattern, pattern);
    }

    public long countAll() {
        Integer n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.Account", Integer.class);
        return n == null ? 0 : n;
    }

    public long countByStatus(String status) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.Account WHERE Status = ?", Integer.class, status);
        return n == null ? 0 : n;
    }

    public long countByRoleName(String roleName) {
        Integer n = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM dbo.Account a
            INNER JOIN dbo.[Role] r ON a.RoleID = r.RoleID
            WHERE UPPER(LTRIM(RTRIM(r.Name))) = UPPER(LTRIM(RTRIM(?)))
            """, Integer.class, roleName);
        return n == null ? 0 : n;
    }

    public int updateRole(int accountId, int roleId) {
        return jdbcTemplate.update("UPDATE dbo.Account SET RoleID = ? WHERE AccountID = ?",
                roleId, accountId);
    }

    // ---------------------------------------------------------- writes

    /**
     * Inserts a new account and returns the generated AccountID.
     * Status defaults to PENDING_VERIFICATION until the activation OTP is verified.
     * PasswordHash is nullable for OAuth-only accounts.
     */
    public Integer insert(Account account) {
        final String sql = """
            INSERT INTO dbo.Account (RoleID, Username, Email, PasswordHash, FullName, Phone, Status)
            OUTPUT inserted.AccountID
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        return jdbcTemplate.queryForObject(sql, Integer.class,
                account.getRoleId(),
                account.getUsername(),
                account.getEmail(),
                account.getPasswordHash(),
                account.getFullName(),
                account.getPhone(),
                account.getStatus() == null ? "PENDING_VERIFICATION" : account.getStatus());
    }

    public int updateStatus(int accountId, String status) {
        return jdbcTemplate.update("UPDATE dbo.Account SET Status = ? WHERE AccountID = ?",
                status, accountId);
    }

    public int updatePasswordHash(int accountId, String passwordHash) {
        return jdbcTemplate.update("UPDATE dbo.Account SET PasswordHash = ? WHERE AccountID = ?",
                passwordHash, accountId);
    }

    public int updateProfile(int accountId, String fullName, String phone) {
        return jdbcTemplate.update("""
                UPDATE dbo.Account SET FullName = ?, Phone = ? WHERE AccountID = ?
                """, fullName, phone, accountId);
    }

    public int markEmailVerified(int accountId) {
        return jdbcTemplate.update(
                "UPDATE dbo.Account SET EmailVerifiedAt = SYSDATETIME(), Status = 'ACTIVE' WHERE AccountID = ?",
                accountId);
    }

    public int touchLastLogin(int accountId) {
        return jdbcTemplate.update(
                "UPDATE dbo.Account SET LastLoginAt = SYSDATETIME() WHERE AccountID = ?",
                accountId);
    }
}

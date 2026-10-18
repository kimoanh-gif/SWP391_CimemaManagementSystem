package com.cinema.dao;

import com.cinema.model.CustomerAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data access for CustomerAccount management (Admin UC-36).
 */
@Repository
public class CustomerAccountDAO {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<CustomerAccount> rowMapper = (rs, rowNum) -> mapResultSetToModel(rs);

    @Autowired
    public CustomerAccountDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private CustomerAccount mapResultSetToModel(ResultSet rs) throws SQLException {
        CustomerAccount ca = new CustomerAccount();

        // Account fields
        ca.setAccountId(rs.getInt("AccountID"));
        ca.setRoleId(rs.getInt("RoleID"));
        ca.setUsername(rs.getString("Username"));
        ca.setEmail(rs.getString("Email"));
        ca.setPasswordHash(rs.getString("PasswordHash"));
        ca.setFullName(rs.getString("FullName"));
        ca.setPhone(rs.getString("Phone"));
        ca.setStatus(rs.getString("Status"));
        ca.setEmailVerified(rs.getTimestamp("EmailVerifiedAt") != null);
        if (rs.getTimestamp("CreatedAt") != null)
            ca.setCreatedAt(rs.getTimestamp("CreatedAt").toLocalDateTime());
        if (rs.getTimestamp("EmailVerifiedAt") != null)
            ca.setEmailVerifiedAt(rs.getTimestamp("EmailVerifiedAt").toLocalDateTime());
        if (rs.getTimestamp("LastLoginAt") != null)
            ca.setLastLoginAt(rs.getTimestamp("LastLoginAt").toLocalDateTime());

        // Customer fields (may be NULL for accounts without Customer profile)
        try { ca.setCustomerId(rs.getInt("CustomerID")); } catch (SQLException ignored) {}
        try { ca.setRankId((Integer) rs.getObject("RankID")); } catch (SQLException ignored) {}
        try {
            if (rs.getDate("DateOfBirth") != null)
                ca.setDateOfBirth(rs.getDate("DateOfBirth").toLocalDate());
        } catch (SQLException ignored) {}
        try { ca.setAddress(rs.getString("Address")); } catch (SQLException ignored) {}
        try { ca.setAvailablePoints(rs.getInt("AvailablePoints")); } catch (SQLException ignored) {}
        try { ca.setTierPoints(rs.getInt("TierPoints")); } catch (SQLException ignored) {}
        try { ca.setReceivePromotionEmail(rs.getBoolean("ReceivePromotionEmail")); } catch (SQLException ignored) {}

        ca.setRoleName(rs.getString("RoleName"));

        return ca;
    }

    private String baseSql() {
        return """
            SELECT a.AccountID, a.RoleID, a.Username, a.Email, a.PasswordHash, a.FullName,
                   a.Phone, a.Status, a.EmailVerifiedAt, a.CreatedAt, a.LastLoginAt,
                   r.Name AS RoleName,
                   c.CustomerID, c.RankID, c.DateOfBirth, c.Address,
                   c.AvailablePoints, c.TierPoints, c.ReceivePromotionEmail
            FROM dbo.Account a
            INNER JOIN dbo.[Role] r ON a.RoleID = r.RoleID
            LEFT JOIN dbo.Customer c ON a.AccountID = c.AccountID
            """;
    }

    // --- Reads ---

    /**
     * UC-36 Normal Flow: List all Customer accounts (legacy – no pagination).
     */
    public List<CustomerAccount> findAllCustomers() {
        return jdbcTemplate.query(baseSql() + """
            WHERE UPPER(LTRIM(RTRIM(r.Name))) = 'CUSTOMER'
            ORDER BY a.AccountID DESC
            """, rowMapper);
    }

    /**
     * UC-36 Normal Flow Step 3: Search customers by keyword.
     */
    public List<CustomerAccount> searchCustomers(String keyword) {
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return jdbcTemplate.query(baseSql() + """
            WHERE UPPER(LTRIM(RTRIM(r.Name))) = 'CUSTOMER'
              AND (LOWER(a.Email) LIKE ?
               OR LOWER(a.Username) LIKE ?
               OR LOWER(a.FullName) LIKE ?
               OR a.Phone LIKE ?)
            ORDER BY a.AccountID DESC
            """, rowMapper, pattern, pattern, pattern, pattern);
    }

    /**
     * UC-36 Normal Flow: List all Customer accounts (paginated).
     */
    public List<CustomerAccount> findAllCustomersPaginated(int page, int size) {
        int offset = (page - 1) * size;
        return jdbcTemplate.query(baseSql() + """
            WHERE UPPER(LTRIM(RTRIM(r.Name))) = 'CUSTOMER'
            ORDER BY a.AccountID DESC
            OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
            """, rowMapper, offset, size);
    }

    /**
     * UC-36 Normal Flow: Search customers by keyword (paginated).
     */
    public List<CustomerAccount> searchCustomersPaginated(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return jdbcTemplate.query(baseSql() + """
            WHERE UPPER(LTRIM(RTRIM(r.Name))) = 'CUSTOMER'
              AND (LOWER(a.Email) LIKE ?
               OR LOWER(a.Username) LIKE ?
               OR LOWER(a.FullName) LIKE ?
               OR a.Phone LIKE ?)
            ORDER BY a.AccountID DESC
            OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
            """, rowMapper, pattern, pattern, pattern, pattern, offset, size);
    }

    /**
     * UC-36 Normal Flow: Count customers matching search keyword.
     */
    public long countBySearch(String keyword) {
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM dbo.Account a
            INNER JOIN dbo.[Role] r ON a.RoleID = r.RoleID
            WHERE UPPER(LTRIM(RTRIM(r.Name))) = 'CUSTOMER'
              AND (LOWER(a.Email) LIKE ?
               OR LOWER(a.Username) LIKE ?
               OR LOWER(a.FullName) LIKE ?
               OR a.Phone LIKE ?)
            """, Long.class, pattern, pattern, pattern, pattern);
    }

    /**
     * UC-36 Normal Flow Step 4: Get account detail.
     * E5: Customer account not found.
     */
    public Optional<CustomerAccount> findById(int accountId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(baseSql() + """
                WHERE a.AccountID = ?
                """, rowMapper, accountId));
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    // --- Uniqueness checks (E1, E2, E3) ---

    public boolean existsByUsername(String username, Integer excludeAccountId) {
        String sql = excludeAccountId != null
            ? "SELECT COUNT(*) FROM dbo.Account WHERE LOWER(LTRIM(RTRIM(Username))) = LOWER(LTRIM(RTRIM(?))) AND AccountID != ?"
            : "SELECT COUNT(*) FROM dbo.Account WHERE LOWER(LTRIM(RTRIM(Username))) = LOWER(LTRIM(RTRIM(?)))";
        return excludeAccountId != null
            ? jdbcTemplate.queryForObject(sql, Integer.class, username, excludeAccountId) > 0
            : jdbcTemplate.queryForObject(sql, Integer.class, username) > 0;
    }

    public boolean existsByEmail(String email, Integer excludeAccountId) {
        String sql = excludeAccountId != null
            ? "SELECT COUNT(*) FROM dbo.Account WHERE LOWER(LTRIM(RTRIM(Email))) = LOWER(LTRIM(RTRIM(?))) AND AccountID != ?"
            : "SELECT COUNT(*) FROM dbo.Account WHERE LOWER(LTRIM(RTRIM(Email))) = LOWER(LTRIM(RTRIM(?)))";
        return excludeAccountId != null
            ? jdbcTemplate.queryForObject(sql, Integer.class, email, excludeAccountId) > 0
            : jdbcTemplate.queryForObject(sql, Integer.class, email) > 0;
    }

    public boolean existsByPhone(String phone, Integer excludeAccountId) {
        String sql = excludeAccountId != null
            ? "SELECT COUNT(*) FROM dbo.Account WHERE LOWER(LTRIM(RTRIM(Phone))) = LOWER(LTRIM(RTRIM(?))) AND AccountID != ?"
            : "SELECT COUNT(*) FROM dbo.Account WHERE LOWER(LTRIM(RTRIM(Phone))) = LOWER(LTRIM(RTRIM(?)))";
        return excludeAccountId != null
            ? jdbcTemplate.queryForObject(sql, Integer.class, phone, excludeAccountId) > 0
            : jdbcTemplate.queryForObject(sql, Integer.class, phone) > 0;
    }

    // --- Writes ---

    /**
     * UC-36 A1: Create new customer account.
     * E1/E2/E3: Uniqueness validation must be done in service layer before calling this.
     */
    public int insertAccount(String username, String email, String passwordHash,
                             String fullName, String phone) {
        String sql = """
            INSERT INTO dbo.Account (RoleID, Username, Email, PasswordHash, FullName, Phone, Status)
            OUTPUT inserted.AccountID
            VALUES (
                (SELECT RoleID FROM dbo.[Role] WHERE UPPER(LTRIM(RTRIM(Name))) = 'CUSTOMER'),
                ?, ?, ?, ?, ?, 'ACTIVE'
            )
            """;
        Integer accountId = jdbcTemplate.queryForObject(sql, Integer.class,
                username, email, passwordHash, fullName, phone);

        // Auto-create Customer profile row
        if (accountId != null) {
            jdbcTemplate.update("""
                INSERT INTO dbo.Customer (AccountID, AvailablePoints, TierPoints, ReceivePromotionEmail)
                VALUES (?, 0, 0, 0)
                """, accountId);
        }
        return accountId != null ? accountId : 0;
    }

    /**
     * UC-36 A2: Update customer account info.
     */
    public int updateAccount(int accountId, String fullName, String phone,
                             String email, String username) {
        return jdbcTemplate.update("""
            UPDATE dbo.Account
            SET FullName = ?, Phone = ?, Email = ?, Username = ?
            WHERE AccountID = ?
            """, fullName, phone, email, username, accountId);
    }

    /**
     * UC-36 A3: Suspend active customer account.
     */
    public int suspendAccount(int accountId) {
        return jdbcTemplate.update(
                "UPDATE dbo.Account SET Status = 'SUSPENDED' WHERE AccountID = ?",
                accountId);
    }

    /**
     * UC-36 A4: Reactivate suspended/disabled customer account.
     */
    public int reactivateAccount(int accountId) {
        return jdbcTemplate.update(
                "UPDATE dbo.Account SET Status = 'ACTIVE' WHERE AccountID = ?",
                accountId);
    }

    /**
     * UC-36 A5: Reset password.
     * E6: Password reset failed.
     */
    public int resetPassword(int accountId, String newPasswordHash) {
        return jdbcTemplate.update(
                "UPDATE dbo.Account SET PasswordHash = ? WHERE AccountID = ?",
                newPasswordHash, accountId);
    }

    // --- Stats ---

    public long countAll() {
        return jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM dbo.Account a
            INNER JOIN dbo.[Role] r ON a.RoleID = r.RoleID
            WHERE UPPER(LTRIM(RTRIM(r.Name))) = 'CUSTOMER'
            """, Long.class);
    }

    public long countByStatus(String status) {
        return jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM dbo.Account a
            INNER JOIN dbo.[Role] r ON a.RoleID = r.RoleID
            WHERE UPPER(LTRIM(RTRIM(r.Name))) = 'CUSTOMER' AND a.Status = ?
            """, Long.class, status);
    }
}

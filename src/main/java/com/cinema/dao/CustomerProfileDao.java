package com.cinema.dao;

import com.cinema.model.CustomerProfile;
import com.cinema.dto.ProfileUpdateForm;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataAccessException;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public class CustomerProfileDao {

    private final JdbcTemplate jdbcTemplate;

    public CustomerProfileDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureProfileSchema();
    }

    private final RowMapper<CustomerProfile> profileRowMapper = (rs, rowNum) -> {
        CustomerProfile profile = new CustomerProfile();

        profile.setAccountId(rs.getInt("AccountID"));
        profile.setCustomerId(rs.getInt("CustomerID"));
        profile.setFullName(rs.getString("FullName"));
        profile.setUsername(rs.getString("Username"));
        profile.setEmail(rs.getString("Email"));
        profile.setPhone(rs.getString("Phone"));

        LocalDate dateOfBirth = rs.getObject("DateOfBirth", LocalDate.class);
        profile.setDateOfBirth(dateOfBirth);

        profile.setMembershipRankName(rs.getString("MembershipRankName"));
        profile.setAvailablePoints(rs.getInt("AvailablePoints"));
        profile.setTierPoints(rs.getInt("TierPoints"));
        profile.setAvatarUrl(rs.getString("AvatarUrl"));
        profile.setPasswordSet(rs.getBoolean("PasswordSet"));

        return profile;
    };

    public Optional<CustomerProfile> findByAccountId(int accountId) {
        String sql = """
                SELECT
                    a.AccountID,
                    c.CustomerID,
                    a.FullName,
                    a.Username,
                    a.Email,
                    a.Phone,
                    c.DateOfBirth,
                    mr.[Name] AS MembershipRankName,
                    c.AvailablePoints,
                    c.TierPoints
                    ,a.AvatarUrl
                    ,CAST(CASE WHEN a.PasswordHash IS NOT NULL AND LTRIM(RTRIM(a.PasswordHash)) <> '' THEN 1 ELSE 0 END AS BIT) PasswordSet
                FROM Account a
                JOIN Customer c ON c.AccountID = a.AccountID
                LEFT JOIN MembershipRank mr ON mr.RankID = c.RankID
                WHERE a.AccountID = ?
                """;

        Optional<CustomerProfile> result = jdbcTemplate.query(sql, profileRowMapper, accountId).stream().findFirst();
        result.ifPresent(profile -> profile.setGoogleLinked(isGoogleLinked(accountId)));
        return result;
    }

    public void updateProfile(int accountId, ProfileUpdateForm form) {
        String updateAccountSql = """
                UPDATE Account
                SET FullName = ?
                WHERE AccountID = ?
                """;

        String updateCustomerSql = """
                UPDATE Customer
                SET DateOfBirth = ?
                WHERE AccountID = ?
                """;

        jdbcTemplate.update(
                updateAccountSql,
                form.getFullName().trim(),
                accountId
        );

        jdbcTemplate.update(
                updateCustomerSql,
                form.getDateOfBirth(), accountId
        );
    }

    public void updateAvatar(int accountId, String avatarUrl) {
        jdbcTemplate.update("UPDATE dbo.Account SET AvatarUrl=? WHERE AccountID=?", avatarUrl, accountId);
    }

    public boolean existsByPhoneExcludingAccount(String phone, int accountId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.Account WHERE Phone=? AND AccountID<>?", Integer.class, phone, accountId);
        return count != null && count > 0;
    }

    private boolean isGoogleLinked(int accountId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM dbo.OAuthAccount WHERE AccountID=? AND Provider='GOOGLE'",
                    Integer.class, accountId);
            return count != null && count > 0;
        } catch (DataAccessException exception) {
            return false;
        }
    }

    private void ensureProfileSchema() {
        try {
            jdbcTemplate.execute("IF COL_LENGTH(N'dbo.Account', N'AvatarUrl') IS NULL ALTER TABLE dbo.Account ADD AvatarUrl NVARCHAR(500) NULL");
        } catch (DataAccessException ignored) {
            // The profile remains readable on restricted database users; avatar upload will report a friendly error.
        }
    }
}

package com.cinema.dao;

import com.cinema.model.CustomerProfile;
import com.cinema.dto.ProfileUpdateForm;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public class CustomerProfileDao {

    private final JdbcTemplate jdbcTemplate;

    public CustomerProfileDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

        profile.setAddress(rs.getString("Address"));
        profile.setMembershipRankName(rs.getString("MembershipRankName"));
        profile.setAvailablePoints(rs.getInt("AvailablePoints"));
        profile.setTierPoints(rs.getInt("TierPoints"));
        profile.setAvatarUrl(rs.getString("AvatarUrl"));
        profile.setGoogleLinked(rs.getBoolean("GoogleLinked"));
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
                    c.[Address] AS Address,
                    mr.[Name] AS MembershipRankName,
                    c.AvailablePoints,
                    c.TierPoints
                    ,a.AvatarUrl
                    ,CAST(CASE WHEN EXISTS (SELECT 1 FROM dbo.OAuthAccount oa WHERE oa.AccountID=a.AccountID AND oa.Provider='GOOGLE') THEN 1 ELSE 0 END AS BIT) GoogleLinked
                    ,CAST(CASE WHEN a.PasswordHash IS NOT NULL AND LTRIM(RTRIM(a.PasswordHash)) <> '' THEN 1 ELSE 0 END AS BIT) PasswordSet
                FROM Account a
                JOIN Customer c ON c.AccountID = a.AccountID
                LEFT JOIN MembershipRank mr ON mr.RankID = c.RankID
                WHERE a.AccountID = ?
                """;

        return jdbcTemplate.query(sql, profileRowMapper, accountId)
                .stream()
                .findFirst();
    }

    public void updateProfile(int accountId, ProfileUpdateForm form) {
        String updateAccountSql = """
                UPDATE Account
                SET FullName = ?
                WHERE AccountID = ?
                """;

        String updateCustomerSql = """
                UPDATE Customer
                SET DateOfBirth = ?, [Address] = ?
                WHERE AccountID = ?
                """;

        jdbcTemplate.update(
                updateAccountSql,
                form.getFullName().trim(),
                accountId
        );

        jdbcTemplate.update(
                updateCustomerSql,
                form.getDateOfBirth(),
                emptyToNull(form.getAddress()),
                accountId
        );
    }

    public void updateAvatar(int accountId, String avatarUrl) {
        jdbcTemplate.update("UPDATE dbo.Account SET AvatarUrl=? WHERE AccountID=?", avatarUrl, accountId);
    }

    public boolean existsByPhoneExcludingAccount(String phone, int accountId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.Account WHERE Phone=? AND AccountID<>?", Integer.class, phone, accountId);
        return count != null && count > 0;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

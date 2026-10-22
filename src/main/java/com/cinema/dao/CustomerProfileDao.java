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
        profile.setEmail(rs.getString("Email"));
        profile.setPhone(rs.getString("Phone"));

        LocalDate dateOfBirth = rs.getObject("DateOfBirth", LocalDate.class);
        profile.setDateOfBirth(dateOfBirth);

        profile.setAddress(rs.getString("Address"));
        profile.setMembershipRankName(rs.getString("MembershipRankName"));
        profile.setAvailablePoints(rs.getInt("AvailablePoints"));
        profile.setTierPoints(rs.getInt("TierPoints"));

        return profile;
    };

    public Optional<CustomerProfile> findByAccountId(int accountId) {
        String sql = """
                SELECT
                    a.AccountID,
                    c.CustomerID,
                    a.FullName,
                    a.Email,
                    a.Phone,
                    c.DateOfBirth,
                    c.[Address] AS Address,
                    mr.[Name] AS MembershipRankName,
                    c.AvailablePoints,
                    c.TierPoints
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
                SET FullName = ?, Phone = ?
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
                emptyToNull(form.getPhone()),
                accountId
        );

        jdbcTemplate.update(
                updateCustomerSql,
                form.getDateOfBirth(),
                emptyToNull(form.getAddress()),
                accountId
        );
    }

    public boolean existsByPhoneExcludingAccount(String phone, int accountId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM dbo.Account
                WHERE Phone = ? AND AccountID <> ?
                """, Integer.class, phone, accountId);
        return count != null && count > 0;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

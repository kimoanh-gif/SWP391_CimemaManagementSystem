package com.cinema.dao;

import com.cinema.model.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.Optional;

@Repository
public class CustomerDAO {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Customer> customerRowMapper = (rs, rowNum) -> mapResultSetToModel(rs);

    @Autowired
    public CustomerDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Customer mapResultSetToModel(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setCustomerId            (rs.getInt("CustomerID"));
        c.setAccountId             (rs.getInt("AccountID"));
        c.setRankId                ((Integer) rs.getObject("RankID"));
        Date dob = rs.getDate("DateOfBirth");
        c.setDateOfBirth           (dob == null ? null : dob.toLocalDate());
        c.setAddress               (rs.getString("Address"));
        c.setAvailablePoints       (rs.getInt("AvailablePoints"));
        c.setTierPoints            (rs.getInt("TierPoints"));
        c.setReceivePromotionEmail (rs.getBoolean("ReceivePromotionEmail"));
        return c;
    }

    public Optional<Customer> findByAccountId(int accountId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                SELECT CustomerID, AccountID, RankID, DateOfBirth, Address,
                       AvailablePoints, TierPoints, ReceivePromotionEmail
                FROM dbo.Customer
                WHERE AccountID = ?
                """, customerRowMapper, accountId));
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * Inserts a new Customer row tied to an existing Account and returns the new CustomerID.
     * RankID is left NULL; the database DEFAULT constraint will assign the default rank.
     */
    public Integer insert(int accountId) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO dbo.Customer (AccountID, AvailablePoints, TierPoints, ReceivePromotionEmail)
            OUTPUT inserted.CustomerID
            VALUES (?, 0, 0, 0)
            """, Integer.class, accountId);
    }
}

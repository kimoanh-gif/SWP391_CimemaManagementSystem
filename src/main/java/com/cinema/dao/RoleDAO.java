package com.cinema.dao;

import com.cinema.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class RoleDAO {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Role> roleRowMapper = (rs, rowNum) -> mapResultSetToModel(rs);

    @Autowired
    public RoleDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Role mapResultSetToModel(ResultSet rs) throws SQLException {
        return new Role(
                rs.getInt("RoleID"),
                rs.getString("Name"),
                rs.getString("Description"));
    }

    public Optional<Role> findByName(String name) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT RoleID, Name, Description FROM dbo.[Role] WHERE UPPER(LTRIM(RTRIM(Name))) = UPPER(LTRIM(RTRIM(?)))",
                    roleRowMapper, name));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Optional<Role> findById(int roleId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT RoleID, Name, Description FROM dbo.[Role] WHERE RoleID = ?",
                    roleRowMapper, roleId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}

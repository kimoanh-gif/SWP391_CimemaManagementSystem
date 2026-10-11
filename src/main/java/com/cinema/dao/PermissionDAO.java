package com.cinema.dao;

import com.cinema.model.Permission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class PermissionDAO {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Permission> permissionRowMapper = (rs, rowNum) -> mapResultSetToModel(rs);

    @Autowired
    public PermissionDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Permission mapResultSetToModel(ResultSet rs) throws SQLException {
        return new Permission(
                rs.getInt("PermissionID"),
                rs.getString("Name"),
                rs.getString("Description"));
    }

    public List<Permission> findAllByRoleId(int roleId) {
        return jdbcTemplate.query("""
                SELECT p.PermissionID, p.Name, p.Description
                FROM dbo.Permission p
                INNER JOIN dbo.RolePermission rp ON rp.PermissionID = p.PermissionID
                WHERE rp.RoleID = ?
                ORDER BY p.Name
                """, permissionRowMapper, roleId);
    }
}

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

    public List<Permission> findAll() {
        return jdbcTemplate.query("""
                SELECT PermissionID, Name, Description
                FROM dbo.Permission
                ORDER BY Name
                """, permissionRowMapper);
    }

    public boolean insertIfMissing(String name, String description) {
        return jdbcTemplate.update("""
                INSERT INTO dbo.Permission (Name, Description)
                SELECT ?, ? WHERE NOT EXISTS (
                    SELECT 1 FROM dbo.Permission WITH (UPDLOCK, HOLDLOCK) WHERE Name = ?
                )
                """, name, description, name) > 0;
    }

    public List<Permission> findByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        String sql = "SELECT PermissionID, Name, Description FROM dbo.Permission WHERE PermissionID IN (" + placeholders + ")";
        return jdbcTemplate.query(sql, permissionRowMapper, ids.toArray());
    }

    public int updateRolePermissions(int roleId, List<Integer> permissionIds) {
        // Delete all existing permissions for this role
        jdbcTemplate.update("DELETE FROM dbo.RolePermission WHERE RoleID = ?", roleId);
        
        // Insert new permissions
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Integer permId : permissionIds) {
                jdbcTemplate.update(
                    "INSERT INTO dbo.RolePermission (RoleID, PermissionID) VALUES (?, ?)",
                    roleId, permId);
            }
        }
        return 1;
    }
}

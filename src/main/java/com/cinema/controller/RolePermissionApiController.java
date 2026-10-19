package com.cinema.controller;

import com.cinema.model.Permission;
import com.cinema.service.RolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST API for Role & Permission Management.
 * All endpoints are prefixed /api/admin/roles
 */
@RestController
@RequestMapping("/api/admin/roles")
public class RolePermissionApiController {

    @Autowired
    private RolePermissionService service;

    /**
     * GET /api/admin/roles
     * Normal Flow Step 2: Display all roles with their permissions.
     */
    @GetMapping
    public ResponseEntity<?> listRoles() {
        var roles = service.getAllRolesWithPermissions();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", roles
        ));
    }

    /**
     * GET /api/admin/roles/{roleId}
     * Normal Flow Step 4: Display permissions for a specific role.
     */
    @GetMapping("/{roleId}")
    public ResponseEntity<?> getRole(@PathVariable int roleId) {
        return service.getRoleWithPermissions(roleId)
                .<ResponseEntity<?>>map(role -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", role
                )))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "code", "NOT_FOUND",
                        "message", "Role not found."
                )));
    }

    /**
     * GET /api/admin/roles/permissions
     * Get all available permissions in the system.
     * Used to populate the "available permissions" list in UI.
     */
    @GetMapping("/permissions")
    public ResponseEntity<?> listPermissions() {
        var permissions = service.getAllPermissions();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", permissions
        ));
    }

    /**
     * PUT /api/admin/roles/{roleId}/permissions
     * Normal Flow Step 5-8: Update role permissions (add/remove).
     * 
     * Body: { "permissions": ["perm1", "perm2", ...] }
     */
    @PutMapping("/{roleId}/permissions")
    public ResponseEntity<?> updatePermissions(
            @PathVariable int roleId,
            @RequestBody Map<String, Object> body) {
        
        @SuppressWarnings("unchecked")
        List<String> permList = (List<String>) body.get("permissions");
        Set<String> permissions = permList != null ? Set.copyOf(permList) : Set.of();

        var result = service.updateRolePermissions(roleId, permissions);
        
        return result.isSuccess()
                ? ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", result.getMessage(),
                        "addedPermissions", result.getAddedPermissions(),
                        "removedPermissions", result.getRemovedPermissions()
                ))
                : ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "code", result.getCode(),
                        "message", result.getMessage()
                ));
    }

    /**
     * POST /api/admin/roles/{roleId}/permissions/add
     * Alternative Flow A1: Add a single permission to a role.
     * 
     * Body: { "permissionName": "movie:read" }
     */
    @PostMapping("/{roleId}/permissions/add")
    public ResponseEntity<?> addPermission(
            @PathVariable int roleId,
            @RequestBody Map<String, String> body) {
        
        String permissionName = body.get("permissionName");
        if (permissionName == null || permissionName.trim().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "code", "INVALID_PERMISSION",
                    "message", "Permission name is required."
            ));
        }

        var result = service.addPermission(roleId, permissionName.trim());
        
        return result.isSuccess()
                ? ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", result.getMessage(),
                        "addedPermissions", result.getAddedPermissions()
                ))
                : ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "code", result.getCode(),
                        "message", result.getMessage()
                ));
    }

    /**
     * POST /api/admin/roles/{roleId}/permissions/remove
     * Alternative Flow A2: Remove a permission from a role.
     * Exception E2: Mandatory permission cannot be removed.
     * 
     * Body: { "permissionName": "movie:read" }
     */
    @PostMapping("/{roleId}/permissions/remove")
    public ResponseEntity<?> removePermission(
            @PathVariable int roleId,
            @RequestBody Map<String, String> body) {
        
        String permissionName = body.get("permissionName");
        if (permissionName == null || permissionName.trim().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "code", "INVALID_PERMISSION",
                    "message", "Permission name is required."
            ));
        }

        // Check if permission is mandatory before attempting removal
        if (!service.canRemovePermission(roleId, permissionName)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "code", "MANDATORY_PERMISSION",
                    "message", "Cannot remove mandatory permission '" + permissionName + "' from this role."
            ));
        }

        var result = service.removePermission(roleId, permissionName.trim());
        
        return result.isSuccess()
                ? ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", result.getMessage(),
                        "removedPermissions", result.getRemovedPermissions()
                ))
                : ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "code", result.getCode(),
                        "message", result.getMessage()
                ));
    }

    /**
     * GET /api/admin/roles/{roleId}/can-remove/{permissionName}
     * Check if a permission can be removed (not mandatory).
     * Used for confirmation dialog (A2).
     */
    @GetMapping("/{roleId}/can-remove/{permissionName}")
    public ResponseEntity<?> canRemovePermission(
            @PathVariable int roleId,
            @PathVariable String permissionName) {
        
        boolean canRemove = service.canRemovePermission(roleId, permissionName);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "canRemove", canRemove,
                "message", canRemove 
                        ? "Permission can be removed." 
                        : "This is a mandatory permission and cannot be removed."
        ));
    }
}

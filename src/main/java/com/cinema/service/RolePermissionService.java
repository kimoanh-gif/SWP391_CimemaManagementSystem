package com.cinema.service;

import com.cinema.dao.PermissionDAO;
import com.cinema.dao.RoleDAO;
import com.cinema.model.Permission;
import com.cinema.model.Role;
import com.cinema.security.RolePermissionPolicy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Role & Permission Management (Admin).
 * Handles CRUD operations for role-permission assignments.
 */
@Service
public class RolePermissionService {

    private final RoleDAO roleDAO;
    private final PermissionDAO permissionDAO;

    /**
     * Mandatory permissions that cannot be removed from roles.
     * Format: RoleName -> Set of mandatory permission names
     */
    private static final Map<String, Set<String>> MANDATORY_PERMISSIONS = RolePermissionPolicy.REQUIRED;

    @Autowired
    public RolePermissionService(RoleDAO roleDAO, PermissionDAO permissionDAO) {
        this.roleDAO = roleDAO;
        this.permissionDAO = permissionDAO;
    }

    // ==================== GET DATA ====================

    /**
     * Get all roles with their current permissions.
     * Normal Flow Step 2: Display available roles.
     */
    public List<RolePermissionVO> getAllRolesWithPermissions() {
        List<Role> roles = roleDAO.findAll();
        List<RolePermissionVO> result = new ArrayList<>();
        
        for (Role role : roles) {
            List<Permission> permissions = permissionDAO.findAllByRoleId(role.getRoleId());
            Set<String> permNames = permissions.stream()
                    .map(Permission::getName)
                    .collect(Collectors.toSet());
            
            result.add(new RolePermissionVO(
                    role.getRoleId(),
                    role.getName(),
                    role.getDescription(),
                    permNames, RolePermissionPolicy.allowed(role.getName()), RolePermissionPolicy.required(role.getName())
            ));
        }
        
        return result;
    }

    /**
     * Get a specific role with its permissions.
     * Normal Flow Step 4: Display permissions for selected role.
     */
    public Optional<RolePermissionVO> getRoleWithPermissions(int roleId) {
        return roleDAO.findById(roleId)
                .map(role -> {
                    List<Permission> permissions = permissionDAO.findAllByRoleId(roleId);
                    Set<String> permNames = permissions.stream()
                            .map(Permission::getName)
                            .collect(Collectors.toSet());
                    return new RolePermissionVO(
                            role.getRoleId(),
                            role.getName(),
                            role.getDescription(),
                            permNames, RolePermissionPolicy.allowed(role.getName()), RolePermissionPolicy.required(role.getName())
                    );
                });
    }

    /**
     * Get all available permissions in the system.
     * Used for populating the "available permissions" list in UI.
     */
    public List<Permission> getAllPermissions() {
        return permissionDAO.findAll();
    }

    /** Remove historical grants outside predefined role boundaries on startup. */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Transactional
    public void removeInvalidAssignments() {
        List<Role> roles = roleDAO.findAll().stream()
                .filter(role -> RolePermissionPolicy.ALLOWED.containsKey(role.getName().toUpperCase(Locale.ROOT)))
                .sorted(Comparator.comparing(Role::getRoleId)).toList();
        // Match the role -> catalog lock order used by interactive permission updates.
        roles.forEach(role -> roleDAO.lockById(role.getRoleId()));
        Set<String> introduced = new HashSet<>();
        // Stable order prevents two application instances locking the catalog in opposite orders.
        new TreeMap<>(RolePermissionPolicy.ADDITIONAL).forEach((name, description) -> {
            if (permissionDAO.insertIfMissing(name, description)) introduced.add(name);
        });
        List<Permission> catalog = permissionDAO.findAll();
        for (Role role : roles) {
            List<Permission> current = permissionDAO.findAllByRoleId(role.getRoleId());
            Set<Integer> valid = current.stream()
                    .filter(p -> RolePermissionPolicy.allowed(role.getName()).contains(p.getName()))
                    .map(Permission::getPermissionId).collect(Collectors.toSet());
            catalog.stream()
                    .filter(p -> RolePermissionPolicy.required(role.getName()).contains(p.getName())
                            || (introduced.contains(p.getName()) && RolePermissionPolicy.allowed(role.getName()).contains(p.getName())))
                    .map(Permission::getPermissionId).forEach(valid::add);
            Set<Integer> previous = current.stream().map(Permission::getPermissionId).collect(Collectors.toSet());
            if (!valid.equals(previous)) permissionDAO.updateRolePermissions(role.getRoleId(), new ArrayList<>(valid));
        }
    }

    // ==================== VALIDATION ====================

    /**
     * Validate permission assignment.
     * Exception E1: Invalid Permission - permission doesn't exist.
     * Exception E3: Invalid Role Configuration.
     */
    public ValidationResult validatePermissions(int roleId, Set<String> newPermissions) {
        // Check role exists
        Optional<Role> roleOpt = roleDAO.findById(roleId);
        if (roleOpt.isEmpty()) {
            return ValidationResult.error("INVALID_ROLE", "Role not found.");
        }
        Role role = roleOpt.get();

        // Check all permission names are valid
        List<Permission> allPermissions = permissionDAO.findAll();
        Set<String> validPermissionNames = allPermissions.stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());

        for (String permName : newPermissions) {
            if (!validPermissionNames.contains(permName)) {
                return ValidationResult.error("INVALID_PERMISSION", 
                        "Permission '" + permName + "' does not exist.");
            }
        }

        if (!RolePermissionPolicy.allowed(role.getName()).containsAll(newPermissions)) {
            return ValidationResult.error("INVALID_ROLE_CONFIGURATION",
                    "This role cannot receive permissions outside its business scope.");
        }

        // Check mandatory permissions (E2)
        Set<String> mandatory = MANDATORY_PERMISSIONS.get(role.getName().toUpperCase());
        if (mandatory != null) {
            for (String req : mandatory) {
                if (!newPermissions.contains(req)) {
                    return ValidationResult.error("MANDATORY_PERMISSION",
                            "Cannot remove mandatory permission '" + req + "' from role '" + role.getName() + "'.");
                }
            }
        }

        return ValidationResult.success();
    }

    /**
     * Check if removing a permission would violate mandatory permission rules.
     * Used for confirmation dialog before removal (A2).
     */
    public boolean canRemovePermission(int roleId, String permissionName) {
        Optional<Role> roleOpt = roleDAO.findById(roleId);
        if (roleOpt.isEmpty()) return false;

        Role role = roleOpt.get();
        Set<String> mandatory = MANDATORY_PERMISSIONS.get(role.getName().toUpperCase());
        
        // If permission is mandatory, cannot remove
        if (mandatory != null && mandatory.contains(permissionName)) {
            return false;
        }
        return true;
    }

    /**
     * Check if a role has a specific permission.
     */
    public boolean roleHasPermission(int roleId, String permissionName) {
        List<Permission> permissions = permissionDAO.findAllByRoleId(roleId);
        return permissions.stream()
                .anyMatch(p -> p.getName().equals(permissionName));
    }

    // ==================== UPDATE PERMISSIONS ====================

    /**
     * Update role permissions.
     * Normal Flow Step 8: Save role-permission configuration.
     * 
     * A1: Add Permission - Admin adds permission to role
     * A2: Remove Permission - Admin removes permission from role
     */
    @Transactional
    public OperationResult updateRolePermissions(int roleId, Set<String> newPermissions) {
        roleDAO.lockById(roleId);
        // Validate first
        ValidationResult validation = validatePermissions(roleId, newPermissions);
        if (!validation.isSuccess()) {
            return OperationResult.error(validation.getCode(), validation.getMessage());
        }

        // Get role name for logging
        String roleName = roleDAO.findById(roleId)
                .map(Role::getName)
                .orElse("Unknown");

        // Get permission IDs from names
        List<Permission> allPermissions = permissionDAO.findAll();
        Map<String, Integer> permNameToId = allPermissions.stream()
                .collect(Collectors.toMap(Permission::getName, Permission::getPermissionId));

        List<Integer> permissionIds = newPermissions.stream()
                .map(permNameToId::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Set<String> oldPermissions = permissionDAO.findAllByRoleId(roleId).stream()
                .map(Permission::getName).collect(Collectors.toSet());
        Set<String> added = new HashSet<>(newPermissions);
        added.removeAll(oldPermissions);
        Set<String> removed = new HashSet<>(oldPermissions);
        removed.removeAll(newPermissions);
        // Update in database
        int result = permissionDAO.updateRolePermissions(roleId, permissionIds);

        if (result > 0) {
            return OperationResult.success(
                    "Permissions updated successfully for role '" + roleName + "'.",
                    added, removed
            );
        } else {
            return OperationResult.error("UPDATE_FAILED", "Failed to update role permissions.");
        }
    }

    /**
     * Add a single permission to a role.
     * Alternative Flow A1.
     */
    @Transactional
    public OperationResult addPermission(int roleId, String permissionName) {
        roleDAO.lockById(roleId);
        // Get current permissions
        Set<String> currentPermissions = permissionDAO.findAllByRoleId(roleId).stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());

        // Check if already has permission
        if (currentPermissions.contains(permissionName)) {
            return OperationResult.error("PERMISSION_EXISTS",
                    "Role already has permission '" + permissionName + "'.");
        }

        // Add new permission
        currentPermissions.add(permissionName);

        return updateRolePermissions(roleId, currentPermissions);
    }

    /**
     * Remove a permission from a role.
     * Alternative Flow A2.
     * Exception E2: Mandatory Permission cannot be removed.
     */
    @Transactional
    public OperationResult removePermission(int roleId, String permissionName) {
        // Check if permission is mandatory (E2)
        if (!canRemovePermission(roleId, permissionName)) {
            Optional<Role> role = roleDAO.findById(roleId);
            String roleName = role.map(Role::getName).orElse("this role");
            return OperationResult.error("MANDATORY_PERMISSION",
                    "Cannot remove mandatory permission '" + permissionName + "' from role '" + roleName + "'.");
        }

        roleDAO.lockById(roleId);
        // Get current permissions
        Set<String> currentPermissions = permissionDAO.findAllByRoleId(roleId).stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());

        // Check if role has this permission
        if (!currentPermissions.contains(permissionName)) {
            return OperationResult.error("PERMISSION_NOT_FOUND",
                    "Role does not have permission '" + permissionName + "'.");
        }

        // Remove permission
        currentPermissions.remove(permissionName);

        return updateRolePermissions(roleId, currentPermissions);
    }

    // ==================== HELPER CLASSES ====================

    /**
     * Value Object representing a role with its permissions.
     */
    public record RolePermissionVO(
            int roleId,
            String roleName,
            String description,
            Set<String> permissions,
            Set<String> allowedPermissions,
            Set<String> mandatoryPermissions
    ) {}

    /**
     * Validation result.
     */
    public static class ValidationResult {
        private final boolean success;
        private final String code;
        private final String message;

        private ValidationResult(boolean success, String code, String message) {
            this.success = success;
            this.code = code;
            this.message = message;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult error(String code, String message) {
            return new ValidationResult(false, code, message);
        }

        public boolean isSuccess() { return success; }
        public String getCode() { return code; }
        public String getMessage() { return message; }
    }

    /**
     * Operation result with details about changes.
     */
    public static class OperationResult {
        private final boolean success;
        private final String code;
        private final String message;
        private final Set<String> addedPermissions;
        private final Set<String> removedPermissions;

        private OperationResult(boolean success, String code, String message,
                               Set<String> addedPermissions, Set<String> removedPermissions) {
            this.success = success;
            this.code = code;
            this.message = message;
            this.addedPermissions = addedPermissions;
            this.removedPermissions = removedPermissions;
        }

        public static OperationResult success(String message, 
                                              Set<String> addedPermissions, 
                                              Set<String> removedPermissions) {
            return new OperationResult(true, null, message, addedPermissions, removedPermissions);
        }

        public static OperationResult error(String code, String message) {
            return new OperationResult(false, code, message, Set.of(), Set.of());
        }

        public boolean isSuccess() { return success; }
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public Set<String> getAddedPermissions() { return addedPermissions; }
        public Set<String> getRemovedPermissions() { return removedPermissions; }
    }
}

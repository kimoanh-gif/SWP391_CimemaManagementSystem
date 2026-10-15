package com.cinema.model;

import java.util.Set;

/**
 * Lightweight principal stored in the HTTP session after successful authentication.
 * Carries only what UI/authorization needs – never the password hash.
 */
public class AuthenticatedUser {

    private final Integer accountId;
    private final String  username;
    private final String  email;
    private final String  fullName;
    private final String  phone;
    private final String  role;          // ADMIN | MANAGER | STAFF | CUSTOMER
    private final Set<String> permissions;
    private final boolean emailVerified;
    private final String  authMethod;    // PASSWORD | OAUTH_GOOGLE

    public AuthenticatedUser(Integer accountId, String username, String email,
                             String fullName, String phone, String role,
                             Set<String> permissions, boolean emailVerified,
                             String authMethod) {
        this.accountId     = accountId;
        this.username      = username;
        this.email         = email;
        this.fullName      = fullName;
        this.phone         = phone;
        this.role          = role;
        this.permissions   = permissions;
        this.emailVerified = emailVerified;
        this.authMethod    = authMethod;
    }

    public Integer getAccountId() { return accountId; }
    public String  getUsername()  { return username; }
    public String  getEmail()     { return email; }
    public String  getFullName()  { return fullName; }
    public String  getPhone()     { return phone; }
    public String  getRole()      { return role; }
    public Set<String> getPermissions() { return permissions; }
    public boolean isEmailVerified() { return emailVerified; }
    public String  getAuthMethod() { return authMethod; }

    public boolean hasRole(String... allowed) {
        if (role == null || allowed == null) return false;
        for (String r : allowed) if (role.equalsIgnoreCase(r)) return true;
        return false;
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}

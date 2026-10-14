package com.cinema.model;

import java.time.LocalDateTime;

/**
 * Pure POJO mapped to dbo.Account.
 * One Account belongs to exactly one Role.
 * Status values: ACTIVE | PENDING_VERIFICATION | LOCKED | DISABLED.
 */
public class Account {
    private Integer accountId;
    private Integer roleId;
    private String  username;
    private String  email;
    private String  passwordHash;
    private String  fullName;
    private String  phone;
    private String  status;
    private LocalDateTime createdAt;

    public Account() {}

    public Account(Integer accountId, Integer roleId, String username, String email,
                   String passwordHash, String fullName, String phone, String status,
                   LocalDateTime createdAt) {
        this.accountId   = accountId;
        this.roleId      = roleId;
        this.username    = username;
        this.email       = email;
        this.passwordHash= passwordHash;
        this.fullName    = fullName;
        this.phone       = phone;
        this.status      = status;
        this.createdAt   = createdAt;
    }

    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }

    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

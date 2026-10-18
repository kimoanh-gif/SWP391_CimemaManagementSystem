package com.cinema.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Customer Account DTO for admin management.
 * Combines Account + Customer profile for display.
 */
public class CustomerAccount {

    // --- Account fields ---
    private Integer accountId;
    private Integer roleId;
    private String  username;
    private String  email;
    private String  passwordHash;
    private String  fullName;
    private String  phone;
    private String  status;
    private boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime emailVerifiedAt;
    private LocalDateTime lastLoginAt;

    // --- Customer fields ---
    private Integer customerId;
    private Integer rankId;
    private LocalDate dateOfBirth;
    private String  address;
    private Integer availablePoints;
    private Integer tierPoints;
    private Boolean receivePromotionEmail;

    // --- Computed ---
    private String roleName;

    public CustomerAccount() {}

    // Getters & Setters (Account)
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

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    // Getters & Setters (Customer)
    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public Integer getRankId() { return rankId; }
    public void setRankId(Integer rankId) { this.rankId = rankId; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Integer getAvailablePoints() { return availablePoints; }
    public void setAvailablePoints(Integer availablePoints) { this.availablePoints = availablePoints; }

    public Integer getTierPoints() { return tierPoints; }
    public void setTierPoints(Integer tierPoints) { this.tierPoints = tierPoints; }

    public Boolean getReceivePromotionEmail() { return receivePromotionEmail; }
    public void setReceivePromotionEmail(Boolean receivePromotionEmail) { this.receivePromotionEmail = receivePromotionEmail; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    // Convenience helpers
    public boolean isActive()     { return "ACTIVE".equals(status); }
    public boolean isLocked()    { return "LOCKED".equals(status) || "SUSPENDED".equals(status); }
    public boolean isPending()    { return "PENDING_VERIFICATION".equals(status); }
    public boolean isDisabled()   { return "DISABLED".equals(status); }
}

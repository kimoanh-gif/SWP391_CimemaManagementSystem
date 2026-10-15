package com.cinema.model;

import java.time.LocalDateTime;

/**
 * POJO for dbo.OtpToken.
 * OTP purpose is restricted to ACTIVATION | RESET_PASSWORD.
 * The raw 6-digit code is NEVER stored – only its BCrypt hash.
 */
public class OtpToken {

    public static final String PURPOSE_ACTIVATION     = "ACTIVATION";
    public static final String PURPOSE_RESET_PASSWORD = "RESET_PASSWORD";

    private Long otpTokenId;
    private Integer accountId;        // nullable: registration flow may not yet have an AccountID
    private String  email;            // always lower-cased
    private String  codeHash;         // BCrypt(strength=10) of the 6-digit code
    private String  purpose;          // ACTIVATION | RESET_PASSWORD
    private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;
    private Integer attemptCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastSentAt;  // for resend cooldown

    public OtpToken() {}

    public Long getOtpTokenId() { return otpTokenId; }
    public void setOtpTokenId(Long otpTokenId) { this.otpTokenId = otpTokenId; }

    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }

    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(LocalDateTime lastSentAt) { this.lastSentAt = lastSentAt; }

    /** True when not yet consumed and not yet expired. */
    public boolean isActive(LocalDateTime now) {
        return consumedAt == null && expiresAt != null && expiresAt.isAfter(now);
    }
}

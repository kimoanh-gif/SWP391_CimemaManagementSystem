package com.cinema.model;

import java.time.LocalDateTime;

/**
 * Audit row for every login attempt (success or failure).
 * Used for rate-limiting, lockout, and brute-force detection.
 * No password / OTP values are ever stored here.
 */
public class LoginAttempt {

    public static final String REASON_OK            = null;
    public static final String REASON_NOT_FOUND     = "NOT_FOUND";
    public static final String REASON_WRONG_PWD     = "WRONG_PASSWORD";
    public static final String REASON_INACTIVE      = "INACTIVE_ACCOUNT";
    public static final String REASON_LOCKED_OUT    = "LOCKED_OUT";
    public static final String REASON_OAUTH         = "OAUTH_SUCCESS";
    public static final String REASON_OAUTH_FAIL    = "OAUTH_FAILED";

    public static final String AUTH_METHOD_PASSWORD    = "PASSWORD";
    public static final String AUTH_METHOD_OAUTH_GOOGLE = "OAUTH_GOOGLE";
    /** Internal placeholder used while we are about to record a password attempt
     *  but the actual method may also be unavailable. The DB column accepts the
     *  same length as the other values. */
    public static final String AUTH_METHOD_PASSWORD_NA = "PASSWORD_NA";

    private Long attemptId;
    private String identifier;       // email or username (lowercased, trimmed)
    private String ipAddress;        // client IP, X-Forwarded-For aware
    private String userAgent;
    private boolean success;
    private String failureReason;    // null when success=true
    private String authMethod;       // PASSWORD | OAUTH_GOOGLE
    private LocalDateTime attemptedAt;

    public LoginAttempt() {}

    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getAuthMethod() { return authMethod; }
    public void setAuthMethod(String authMethod) { this.authMethod = authMethod; }

    public LocalDateTime getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(LocalDateTime attemptedAt) { this.attemptedAt = attemptedAt; }
}

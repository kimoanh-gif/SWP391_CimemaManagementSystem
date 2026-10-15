package com.cinema.model;

import java.time.LocalDateTime;

/**
 * Links a local Account to an external identity provider (e.g. Google).
 * 1 Account ↔ N OAuthAccount (one local account may be linked to several providers).
 * (Provider, ProviderSubject) is unique – a given external identity maps to at most one local account.
 */
public class OAuthAccount {

    public static final String PROVIDER_GOOGLE = "GOOGLE";

    private Long oAuthAccountId;
    private Integer accountId;          // FK -> dbo.Account
    private String  provider;           // GOOGLE
    private String  providerSubject;    // OIDC `sub` claim – globally unique per provider
    private String  providerEmail;      // last-seen email from the provider
    private boolean emailVerifiedByProvider; // true if `email_verified` was true in ID token
    private LocalDateTime linkedAt;
    private LocalDateTime lastLoginAt;

    public OAuthAccount() {}

    public Long getOAuthAccountId() { return oAuthAccountId; }
    public void setOAuthAccountId(Long oAuthAccountId) { this.oAuthAccountId = oAuthAccountId; }

    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderSubject() { return providerSubject; }
    public void setProviderSubject(String providerSubject) { this.providerSubject = providerSubject; }

    public String getProviderEmail() { return providerEmail; }
    public void setProviderEmail(String providerEmail) { this.providerEmail = providerEmail; }

    public boolean isEmailVerifiedByProvider() { return emailVerifiedByProvider; }
    public void setEmailVerifiedByProvider(boolean emailVerifiedByProvider) { this.emailVerifiedByProvider = emailVerifiedByProvider; }

    public LocalDateTime getLinkedAt() { return linkedAt; }
    public void setLinkedAt(LocalDateTime linkedAt) { this.linkedAt = linkedAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}

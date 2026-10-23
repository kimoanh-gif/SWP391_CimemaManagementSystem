package com.cinema.service;

import com.cinema.dao.AccountDAO;
import com.cinema.dao.CustomerDAO;
import com.cinema.dao.LoginAttemptDAO;
import com.cinema.dao.OAuthAccountDAO;
import com.cinema.dao.OtpTokenDAO;
import com.cinema.dao.RoleDAO;
import com.cinema.model.Account;
import com.cinema.model.LoginAttempt;
import com.cinema.model.OAuthAccount;
import com.cinema.model.OtpToken;
import com.cinema.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Authentication & registration business logic.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Self-registration: validates uniqueness, hashes password, creates Account + Customer, sends activation OTP.</li>
 *   <li>Activation: verifies OTP, flips Account.Status to ACTIVE.</li>
 *   <li>Login: password verification, status check, rate-limit, lockout, audit, generic error.</li>
 *   <li>Forgot/Reset password: OTP-based recovery, one-time use, audit.</li>
 *   <li>OAuth mapping: link Google subject → local Account, create Account on first Google login (CUSTOMER only).</li>
 * </ul>
 */
@Service
public class AuthService {

    private static final Pattern EMAIL_RE    = com.cinema.util.AccountValidation.EMAIL;
    private static final Pattern PHONE_RE    = com.cinema.util.AccountValidation.PHONE;
    private static final Pattern USERNAME_RE = com.cinema.util.AccountValidation.USERNAME;
    private static final Pattern PASSWORD_RE = com.cinema.util.AccountValidation.PASSWORD;

    private final AccountDAO       accountDAO;
    private final RoleDAO          roleDAO;
    private final CustomerDAO      customerDAO;
    private final OtpTokenDAO      otpTokenDAO;
    private final LoginAttemptDAO  loginAttemptDAO;
    private final OAuthAccountDAO  oAuthAccountDAO;
    private final OtpService       otpService;
    private final PasswordEncoder  passwordEncoder;

    @Value("${app.auth.otp.max-attempts:5}")
    private int maxOtpAttempts = 5;

    @Value("${app.auth.login.max-failures:5}")
    private int maxLoginFailures = 5;

    @Value("${app.auth.login.lockout-minutes:5}")
    private int lockoutMinutes = 5;

    @Value("${app.auth.login.max-failures-per-ip:20}")
    private int maxLoginFailuresPerIp = 20;

    public AuthService(AccountDAO accountDAO, RoleDAO roleDAO, CustomerDAO customerDAO,
                       OtpTokenDAO otpTokenDAO, LoginAttemptDAO loginAttemptDAO,
                       OAuthAccountDAO oAuthAccountDAO, OtpService otpService,
                       PasswordEncoder passwordEncoder) {
        this.accountDAO      = accountDAO;
        this.roleDAO         = roleDAO;
        this.customerDAO     = customerDAO;
        this.otpTokenDAO     = otpTokenDAO;
        this.loginAttemptDAO = loginAttemptDAO;
        this.oAuthAccountDAO = oAuthAccountDAO;
        this.otpService      = otpService;
        this.passwordEncoder = passwordEncoder;
    }

    /** Propagates out of the service transaction so partial registration is rolled back. */
    public static class RegistrationFailure extends RuntimeException {
        private final String status;
        public RegistrationFailure(String status, Throwable cause) {
            super("Registration could not be completed.", cause);
            this.status = status;
        }
        public RegisterResult result() { return new RegisterResult(status, null); }
    }

    // ============================================================ register
    public record RegisterResult(String status, String message) {
        public static RegisterResult ok()                         { return new RegisterResult("OK", null); }
        public static RegisterResult emailTaken()                { return new RegisterResult("EMAIL_TAKEN", null); }
        public static RegisterResult usernameTaken()             { return new RegisterResult("USERNAME_TAKEN", null); }
        public static RegisterResult phoneTaken()                { return new RegisterResult("PHONE_TAKEN", null); }
        public static RegisterResult invalidEmail()               { return new RegisterResult("INVALID_EMAIL", null); }
        public static RegisterResult invalidUsername()           { return new RegisterResult("INVALID_USERNAME", null); }
        public static RegisterResult invalidPhone()              { return new RegisterResult("INVALID_PHONE", null); }
        public static RegisterResult invalidPassword()           { return new RegisterResult("INVALID_PASSWORD", null); }
        public static RegisterResult invalidFullname()           { return new RegisterResult("INVALID_FULLNAME", null); }
        public static RegisterResult otpDeliveryFailed() { return new RegisterResult("OTP_DELIVERY_FAILED", null); }
        public static RegisterResult roleNotConfigured()         { return new RegisterResult("ROLE_NOT_CONFIGURED", null); }
        public static RegisterResult otpLimitExceeded(String msg){ return new RegisterResult("OTP_LIMIT_EXCEEDED", msg); }
    }

    /**
     * Registers a new customer account. Returns SUCCESS only when the activation
     * OTP has been sent successfully.
     */
    @Transactional
    public RegisterResult register(String username, String email, String rawPassword,
                                   String fullName, String phone) {

        if (username == null || !USERNAME_RE.matcher(username).matches())
            return RegisterResult.invalidUsername();
        if (email == null || !EMAIL_RE.matcher(email.trim()).matches())
            return RegisterResult.invalidEmail();
        if (fullName == null || fullName.trim().length() < 2 || fullName.trim().length() > 100)
            return RegisterResult.invalidFullname();
        if (phone != null && !phone.isBlank() && !PHONE_RE.matcher(phone.trim()).matches())
            return RegisterResult.invalidPhone();
        if (rawPassword == null || !PASSWORD_RE.matcher(rawPassword).matches())
            return RegisterResult.invalidPassword();

        email = email.trim().toLowerCase(Locale.ROOT);
        String limitMsg = otpService.canSendOtp(email, OtpToken.PURPOSE_ACTIVATION);
        if (limitMsg != null) return RegisterResult.otpLimitExceeded(limitMsg);

        if (accountDAO.existsByEmail(email))     return RegisterResult.emailTaken();
        if (accountDAO.existsByUsername(username)) return RegisterResult.usernameTaken();
        if (phone != null && !phone.isBlank() && accountDAO.existsByPhone(phone.trim()))
            return RegisterResult.phoneTaken();

        Role role = roleDAO.findByName("CUSTOMER").orElse(null);
        if (role == null) return RegisterResult.roleNotConfigured();

        Account acc = new Account();
        acc.setRoleId(role.getRoleId());
        acc.setUsername(username.trim());
        acc.setEmail(email.trim().toLowerCase(Locale.ROOT));
        acc.setPasswordHash(passwordEncoder.encode(rawPassword));
        acc.setFullName(fullName.trim());
        acc.setPhone((phone == null || phone.isBlank()) ? null : phone.trim());
        acc.setStatus("PENDING_VERIFICATION");

        Integer newId;
        try {
            newId = accountDAO.insert(acc);
        } catch (DuplicateKeyException ex) {
            // Do not query a potentially aborted transaction to identify a duplicate.
            String detail = ex.getMostSpecificCause().getMessage();
            String constraint = detail == null ? "" : detail.toLowerCase(Locale.ROOT);
            String status = constraint.contains("uq_account_username") ? "USERNAME_TAKEN"
                    : constraint.contains("uq_account_email") ? "EMAIL_TAKEN"
                    : constraint.contains("ux_account_phone") ? "PHONE_TAKEN" : "REGISTRATION_FAILED";
            throw new RegistrationFailure(status, ex);
        }
        if (newId == null || newId <= 0) throw new RegistrationFailure("REGISTRATION_FAILED", null);

        try {
            Integer customerId = customerDAO.insert(newId);
            if (customerId == null || customerId <= 0) {
                throw new RegistrationFailure("REGISTRATION_FAILED", null);
            }
        } catch (org.springframework.dao.DataAccessException ex) {
            throw new RegistrationFailure("REGISTRATION_FAILED", ex);
        }

        RegisterResult result = dispatchOtp(newId, acc.getEmail(), OtpToken.PURPOSE_ACTIVATION);
        if ("OTP_LIMIT_EXCEEDED".equals(result.status())) {
            // Recheck may reject a concurrent send; roll back the account as well.
            throw new RegistrationFailure(result.status(), null);
        }
        return result;
    }

    /** Plain OTP is only passed to the email service, never returned to callers. */
    private RegisterResult dispatchOtp(Integer accountId, String email, String purpose) {
        // Check daily send limit
        String limitMsg = otpService.canSendOtp(email, purpose);
        if (limitMsg != null) {
            return RegisterResult.otpLimitExceeded(limitMsg);
        }

        // Drop any prior unconsumed tokens for this (email, purpose).
        otpTokenDAO.deleteAllByEmailAndPurpose(email, purpose);

        String code = otpService.generateCode();
        OtpToken t = new OtpToken();
        t.setAccountId(accountId);
        t.setEmail(email);
        t.setCodeHash(otpService.hash(code));
        t.setPurpose(purpose);
        t.setExpiresAt(LocalDateTime.now().plusSeconds(otpService.getOtpTtlSeconds()));
        t.setLastSentAt(LocalDateTime.now());
        t.setSendCount(1);
        t.setSendCountDate(LocalDateTime.now());
        Long tokenId = otpTokenDAO.insert(t);

        // Track send count
        if (tokenId != null) {
            otpTokenDAO.incrementSendCount(tokenId);
        }

        if (!otpService.sendByEmail(email, code, purpose)) {
            return RegisterResult.otpDeliveryFailed();
        }
        return RegisterResult.ok();
    }

    // ============================================================= activate
    public enum OtpResult { SUCCESS, INVALID_OR_EXPIRED, TOO_MANY_ATTEMPTS, NO_PENDING_TOKEN }

    @Transactional
    public OtpResult activate(String email, String code) {
        return verifyOtpAndConsume(email, code, OtpToken.PURPOSE_ACTIVATION,
                accountId -> { accountDAO.markEmailVerified(accountId); return 0; });
    }

    // ================================================================= login
    public enum LoginResultStatus {
        SUCCESS, EMAIL_NOT_FOUND, WRONG_PASSWORD, INACTIVE_ACCOUNT,
        UNSUPPORTED_ROLE, LOCKED_OUT, TOO_MANY_IP_FAILURES, OAUTH_PASSWORD_NOT_SET
    }

    public record LoginResult(LoginResultStatus status, Account account, int retryAfterSeconds) {
        public static LoginResult of(LoginResultStatus s) { return new LoginResult(s, null, 0); }
        public static LoginResult of(LoginResultStatus s, int retryAfter) { return new LoginResult(s, null, retryAfter); }
        public static LoginResult success(Account a)      { return new LoginResult(LoginResultStatus.SUCCESS, a, 0); }
    }

    @Transactional
    public LoginResult login(String identifier, String rawPassword,
                             String ipAddress, String userAgent) {
        if (identifier == null || rawPassword == null
                || identifier.isBlank() || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Thiếu thông tin đăng nhập.");
        }
        String normalized = identifier.trim().toLowerCase(Locale.ROOT);

        // IP-level rate limit
        int ipFailures = loginAttemptDAO.countRecentFailuresByIp(ipAddress, lockoutMinutes);
        if (ipFailures >= maxLoginFailuresPerIp) {
            int retry = computeRetryAfterSeconds(normalized);
            recordAttempt(normalized, ipAddress, userAgent, false,
                    LoginAttempt.REASON_LOCKED_OUT, LoginAttempt.AUTH_METHOD_PASSWORD_NA);
            return LoginResult.of(LoginResultStatus.TOO_MANY_IP_FAILURES, retry);
        }

        // Identifier-level lockout
        int idFailures = loginAttemptDAO.countRecentFailuresByIdentifier(normalized, lockoutMinutes);
        if (idFailures >= maxLoginFailures) {
            int retry = computeRetryAfterSeconds(normalized);
            recordAttempt(normalized, ipAddress, userAgent, false,
                    LoginAttempt.REASON_LOCKED_OUT, LoginAttempt.AUTH_METHOD_PASSWORD_NA);
            return LoginResult.of(LoginResultStatus.LOCKED_OUT, retry);
        }

        Account account = accountDAO.findByEmailOrUsername(identifier).orElse(null);
        if (account == null) {
            slowDown();
            recordAttempt(normalized, ipAddress, userAgent, false,
                    LoginAttempt.REASON_NOT_FOUND, LoginAttempt.AUTH_METHOD_PASSWORD_NA);
            return LoginResult.of(LoginResultStatus.EMAIL_NOT_FOUND);
        }

        String hash = account.getPasswordHash();
        // OAuth-only account – no password set
        if (hash == null || hash.isBlank()) {
            recordAttempt(normalized, ipAddress, userAgent, false,
                    LoginAttempt.REASON_WRONG_PWD, LoginAttempt.AUTH_METHOD_PASSWORD_NA);
            return LoginResult.of(LoginResultStatus.OAUTH_PASSWORD_NOT_SET);
        }
        if (!(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"))) {
            slowDown();
            recordAttempt(normalized, ipAddress, userAgent, false,
                    LoginAttempt.REASON_WRONG_PWD, LoginAttempt.AUTH_METHOD_PASSWORD_NA);
            return LoginResult.of(LoginResultStatus.WRONG_PASSWORD);
        }
        if (!passwordEncoder.matches(rawPassword, hash)) {
            slowDown();
            recordAttempt(normalized, ipAddress, userAgent, false,
                    LoginAttempt.REASON_WRONG_PWD, LoginAttempt.AUTH_METHOD_PASSWORD_NA);
            return LoginResult.of(LoginResultStatus.WRONG_PASSWORD);
        }
        String status = account.getStatus() == null ? "" : account.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(status)) {
            recordAttempt(normalized, ipAddress, userAgent, false,
                    LoginAttempt.REASON_INACTIVE, LoginAttempt.AUTH_METHOD_PASSWORD_NA);
            // Preserve the account so the controller can resume activation for
            // accounts that are still waiting for OTP verification.
            return new LoginResult(LoginResultStatus.INACTIVE_ACCOUNT, account, 0);
        }

        accountDAO.touchLastLogin(account.getAccountId());
        recordAttempt(normalized, ipAddress, userAgent, true,
                LoginAttempt.REASON_OK, LoginAttempt.AUTH_METHOD_PASSWORD_NA);
        return LoginResult.success(account);
    }

    private int computeRetryAfterSeconds(String identifier) {
        LocalDateTime last = loginAttemptDAO.findLastFailureTime(identifier);
        if (last == null) return lockoutMinutes * 60;
        long elapsed = Duration.between(last, LocalDateTime.now()).getSeconds();
        long total   = (long) lockoutMinutes * 60L;
        long remain  = total - elapsed;
        return remain > 0 ? (int) remain : 0;
    }

    private void slowDown() {
        try { Thread.sleep(50); } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void recordAttempt(String identifier, String ip, String ua,
                               boolean ok, String reason, String method) {
        try {
            LoginAttempt a = new LoginAttempt();
            a.setIdentifier(identifier);
            a.setIpAddress(ip);
            a.setUserAgent(ua);
            a.setSuccess(ok);
            a.setFailureReason(ok ? null : reason);
            a.setAuthMethod(method);
            loginAttemptDAO.insert(a);
        } catch (Exception ex) {
            // Audit failure must never break login.
        }
    }

    // =================================================== forgot / reset
    @Transactional
    public boolean requestPasswordReset(String email) {
        if (email == null || email.isBlank()) return true;
        Optional<Account> opt = accountDAO.findByEmail(email.trim());
        if (opt.isEmpty()) return true;
        Account account = opt.get();
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) return true;
        dispatchOtp(account.getAccountId(), account.getEmail(), OtpToken.PURPOSE_RESET_PASSWORD);
        return true;
    }

    @Transactional
    public OtpResult resetPassword(String email, String code, String newPassword) {
        if (newPassword == null || !PASSWORD_RE.matcher(newPassword).matches()) {
            return OtpResult.INVALID_OR_EXPIRED;
        }
        return verifyOtpAndConsume(email, code, OtpToken.PURPOSE_RESET_PASSWORD,
                accountId -> {
                    accountDAO.updatePasswordHash(accountId, passwordEncoder.encode(newPassword));
                    return 0;
                });
    }

    // =================================================== change password (logged-in)
    @Transactional
    public boolean changePassword(int accountId, String oldPassword, String newPassword) {
        if (newPassword == null || !PASSWORD_RE.matcher(newPassword).matches()) return false;
        Account acc = accountDAO.findById(accountId).orElse(null);
        if (acc == null || acc.getPasswordHash() == null) return false;
        if (!passwordEncoder.matches(oldPassword, acc.getPasswordHash())) return false;
        accountDAO.updatePasswordHash(accountId, passwordEncoder.encode(newPassword));
        return true;
    }

    @Transactional
    public boolean setInitialGooglePassword(int accountId, String newPassword) {
        if (newPassword == null || !PASSWORD_RE.matcher(newPassword).matches()) return false;
        Account account = accountDAO.findById(accountId).orElse(null);
        if (account == null || account.getPasswordHash() != null) return false;
        boolean googleLinked = oAuthAccountDAO.findAllByAccountId(accountId).stream()
                .anyMatch(link -> OAuthAccount.PROVIDER_GOOGLE.equalsIgnoreCase(link.getProvider()));
        if (!googleLinked) return false;
        return accountDAO.updatePasswordHash(accountId, passwordEncoder.encode(newPassword)) == 1;
    }

    // =========================================== OTP pre-check (no consume)
    @Transactional
    public OtpResult preCheckOtp(String email, String code, String purpose) {
        if (email == null || code == null) return OtpResult.INVALID_OR_EXPIRED;
        OtpToken t = otpTokenDAO.findActiveByEmailAndPurpose(email.trim(), purpose).orElse(null);
        if (t == null) return OtpResult.NO_PENDING_TOKEN;
        if (t.getAttemptCount() != null && t.getAttemptCount() >= maxOtpAttempts) {
            otpTokenDAO.markConsumed(t.getOtpTokenId());
            return OtpResult.TOO_MANY_ATTEMPTS;
        }
        if (!otpService.verify(code, t.getCodeHash())) {
            otpTokenDAO.incrementAttempt(t.getOtpTokenId());
            return OtpResult.INVALID_OR_EXPIRED;
        }
        return OtpResult.SUCCESS;
    }

    /** Resend an OTP for the given email + purpose if cooldown allows. */
    @Transactional
    public ResendResult resendOtp(String email, String purpose) {
        OtpToken latest = otpTokenDAO.findLatestByEmailAndPurpose(email.trim().toLowerCase(Locale.ROOT), purpose).orElse(null);
        if (latest == null) {
            return new ResendResult(false, 0, "NO_PENDING_TOKEN", null);
        }
        if (!otpService.canResend(latest.getLastSentAt())) {
            return new ResendResult(false, otpService.secondsUntilNextResend(latest.getLastSentAt()), "COOLDOWN", null);
        }
        // We assume latest is still active – reissue code.
        RegisterResult result = dispatchOtp(latest.getAccountId(), email.trim().toLowerCase(Locale.ROOT), purpose);
        if ("OK".equals(result.status())) {
            return new ResendResult(true, 0, null, null);
        } else {
            return new ResendResult(false, 0, result.status(), result.message());
        }
    }

    public record ResendResult(boolean sent, int retryAfterSeconds, String code, String message) {}

    // =============================================================== helpers

    private OtpResult verifyOtpAndConsume(String email, String code, String purpose,
                                          Function<Integer, Integer> sideEffect) {
        if (email == null || code == null) return OtpResult.INVALID_OR_EXPIRED;
        OtpToken t = otpTokenDAO.findActiveByEmailAndPurpose(email.trim(), purpose).orElse(null);
        if (t == null) return OtpResult.NO_PENDING_TOKEN;
        if (t.getAttemptCount() != null && t.getAttemptCount() >= maxOtpAttempts) {
            otpTokenDAO.markConsumed(t.getOtpTokenId());
            return OtpResult.TOO_MANY_ATTEMPTS;
        }
        if (!otpService.verify(code, t.getCodeHash())) {
            otpTokenDAO.incrementAttempt(t.getOtpTokenId());
            return OtpResult.INVALID_OR_EXPIRED;
        }
        otpTokenDAO.markConsumed(t.getOtpTokenId());
        
        // If AccountID is NULL in OtpToken, try to find account by email as backup
        Integer accountId = t.getAccountId();
        if (accountId == null) {
            Optional<Account> account = accountDAO.findByEmail(email.trim());
            if (account.isPresent()) {
                accountId = account.get().getAccountId();
            }
        }
        
        if (accountId != null) {
            sideEffect.apply(accountId);
        }
        return OtpResult.SUCCESS;
    }

    // =================================================== OAuth (Google)

    /**
     * Returns the local account linked to the given Google identity, if any.
     */
    public Optional<Account> findAccountByGoogleSubject(String subject) {
        return oAuthAccountDAO.findByProviderSubject(OAuthAccount.PROVIDER_GOOGLE, subject)
                .flatMap(o -> accountDAO.findById(o.getAccountId()));
    }

    /**
     * Find a local account by email address.
     */
    public Optional<Account> findAccountByEmail(String email) {
        return accountDAO.findByEmail(email);
    }

    /**
     * Link a Google identity to an existing local account. Used when the user
     * explicitly signs in with Google and we already have an account with the
     * same verified email.
     */
    @Transactional
    public void linkGoogleIdentity(int accountId, String subject, String email, boolean emailVerified) {
        OAuthAccount link = new OAuthAccount();
        link.setAccountId(accountId);
        link.setProvider(OAuthAccount.PROVIDER_GOOGLE);
        link.setProviderSubject(subject);
        link.setProviderEmail(email);
        link.setEmailVerifiedByProvider(emailVerified);
        oAuthAccountDAO.insert(link);
    }

    /**
     * Create a fresh local account for a new Google user. CUSTOMER role only.
     * Username is derived from the email; if taken, a numeric suffix is appended.
     */
    @Transactional
    public Account createGoogleAccount(String email, String fullName, String subject, boolean emailVerified) {
        Role role = roleDAO.findByName("CUSTOMER").orElseThrow(
                () -> new IllegalStateException("CUSTOMER role not configured"));
        String username = deriveUsernameFromEmail(email);
        Account acc = new Account();
        acc.setRoleId(role.getRoleId());
        acc.setUsername(username);
        acc.setEmail(email);
        acc.setPasswordHash(null); // OAuth-only
        acc.setFullName(fullName == null || fullName.isBlank() ? username : fullName);
        acc.setPhone(null);
        acc.setStatus("ACTIVE"); // Google identity is provider-verified
        Integer id = accountDAO.insert(acc);
        acc.setAccountId(id);

        // Create matching Customer row.
        try { customerDAO.insert(id); }
        catch (DuplicateKeyException ignored) { /* already exists */ }

        // Link OAuth identity.
        linkGoogleIdentity(id, subject, email, emailVerified);
        return acc;
    }

    private String deriveUsernameFromEmail(String email) {
        String local = email == null ? "user" : email.split("@", 2)[0].toLowerCase(Locale.ROOT);
        local = local.replaceAll("[^A-Za-z0-9_.-]", "");
        if (local.length() < 4) local = (local + "user").substring(0, 4);
        if (local.length() > 40) local = local.substring(0, 40);
        String candidate = local;
        int suffix = 0;
        while (accountDAO.existsByUsername(candidate)) {
            suffix++;
            candidate = local + suffix;
            if (candidate.length() > 50) candidate = local.substring(0, 50 - String.valueOf(suffix).length()) + suffix;
        }
        return candidate;
    }
}

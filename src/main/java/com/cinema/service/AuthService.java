package com.cinema.service;

import com.cinema.dao.AccountDAO;
import com.cinema.dao.CustomerDAO;
import com.cinema.dao.OtpTokenDAO;
import com.cinema.dao.RoleDAO;
import com.cinema.model.Account;
import com.cinema.model.OtpToken;
import com.cinema.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Authentication & registration business logic.
 *   - register: validates uniqueness, hashes password, creates Account + Customer, sends OTP.
 *   - activate: verifies OTP, flips Account.Status to ACTIVE.
 *   - login:   verifies credentials + status.
 *   - forgotPassword / resetPassword: OTP-based password recovery.
 *   - changePassword: requires current password.
 *
 * Thread-safety: this class holds no mutable state. All DAO calls go through
 * the JdbcTemplate (thread-safe) and BCrypt hashing is CPU-bound but stateless.
 *
 * Important: every public method is @Transactional so the OTP-verify-then-side-effect
 * pair behaves atomically. Private helpers are joined to the caller's transaction.
 */
@Service
public class AuthService {

    private static final Pattern EMAIL_RE = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_RE = Pattern.compile("^(0|\\+84)[0-9]{9,10}$");
    private static final Pattern USERNAME_RE = Pattern.compile("^[A-Za-z0-9_.-]{4,50}$");
    private static final Pattern PASSWORD_RE = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");

    /** Single shared encoder – strength 10, same as OtpService. */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    private final AccountDAO  accountDAO;
    private final RoleDAO     roleDAO;
    private final CustomerDAO customerDAO;
    private final OtpTokenDAO otpTokenDAO;
    private final OtpService  otpService;

    @Value("${app.auth.otp.max-attempts:5}")
    private int maxOtpAttempts;

    public AuthService(AccountDAO accountDAO, RoleDAO roleDAO, CustomerDAO customerDAO,
                       OtpTokenDAO otpTokenDAO, OtpService otpService) {
        this.accountDAO  = accountDAO;
        this.roleDAO     = roleDAO;
        this.customerDAO = customerDAO;
        this.otpTokenDAO = otpTokenDAO;
        this.otpService  = otpService;
    }

    // ============================================================ register
    public enum RegisterResult {
        SUCCESS,                       // OTP sent, waiting for activation
        EMAIL_TAKEN,
        USERNAME_TAKEN,
        INVALID_EMAIL,
        INVALID_USERNAME,
        INVALID_PHONE,
        INVALID_PASSWORD,
        OTP_DELIVERY_FAILED,
        ROLE_NOT_CONFIGURED
    }

    /**
     * Registers a new customer account and emails an activation OTP.
     * The new account is created with Status = PENDING_VERIFICATION and only
     * becomes ACTIVE after the activation OTP is verified.
     */
    @Transactional
    public RegisterResult register(String username, String email, String rawPassword,
                                   String fullName, String phone) {

        // --- input validation ---
        if (username == null || !USERNAME_RE.matcher(username).matches())
            return RegisterResult.INVALID_USERNAME;
        if (email == null || !EMAIL_RE.matcher(email.trim()).matches())
            return RegisterResult.INVALID_EMAIL;
        if (phone != null && !phone.isBlank() && !PHONE_RE.matcher(phone.trim()).matches())
            return RegisterResult.INVALID_PHONE;
        if (rawPassword == null || rawPassword.length() < 8 || !PASSWORD_RE.matcher(rawPassword).matches())
            return RegisterResult.INVALID_PASSWORD;

        // --- uniqueness checks ---
        if (accountDAO.existsByEmail(email))     return RegisterResult.EMAIL_TAKEN;
        if (accountDAO.existsByUsername(username)) return RegisterResult.USERNAME_TAKEN;

        // --- role lookup ---
        Role role = roleDAO.findByName("CUSTOMER").orElse(null);
        if (role == null) return RegisterResult.ROLE_NOT_CONFIGURED;

        // --- build account ---
        Account acc = new Account();
        acc.setRoleId(role.getRoleId());
        acc.setUsername(username.trim());
        acc.setEmail(email.trim().toLowerCase());
        acc.setPasswordHash(passwordEncoder.encode(rawPassword));
        acc.setFullName(fullName == null ? "" : fullName.trim());
        acc.setPhone((phone == null || phone.isBlank()) ? null : phone.trim());
        acc.setStatus("PENDING_VERIFICATION");

        Integer newId = accountDAO.insert(acc);
        if (newId == null) return RegisterResult.ROLE_NOT_CONFIGURED;

        // --- create matching Customer row (non-fatal if it already exists) ---
        try {
            customerDAO.insert(newId);
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            // customer already exists for this account; continue.
        }

        // --- dispatch OTP ---
        return dispatchOtp(newId, acc.getEmail(), OtpToken.PURPOSE_ACTIVATION);
    }

    // ============================================================= activate
    public enum OtpResult { SUCCESS, INVALID_OR_EXPIRED, TOO_MANY_ATTEMPTS, NO_PENDING_TOKEN }

    /** Verifies the activation OTP and flips the account to ACTIVE. */
    @Transactional
    public OtpResult activate(String email, String code) {
        return verifyOtpAndConsume(email, code, OtpToken.PURPOSE_ACTIVATION,
                accountId -> accountDAO.updateStatus(accountId, "ACTIVE"));
    }

    // ================================================================= login
    public enum LoginResultStatus {
        SUCCESS, EMAIL_NOT_FOUND, WRONG_PASSWORD, INACTIVE_ACCOUNT, UNSUPPORTED_ROLE
    }

    public record LoginResult(LoginResultStatus status, Account account) {
        public static LoginResult of(LoginResultStatus s) { return new LoginResult(s, null); }
        public static LoginResult success(Account a)      { return new LoginResult(LoginResultStatus.SUCCESS, a); }
    }

    public LoginResult login(String identifier, String rawPassword) {
        if (identifier == null || rawPassword == null
                || identifier.isBlank() || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Thiếu thông tin đăng nhập.");
        }
        Account account = accountDAO.findByEmailOrUsername(identifier).orElse(null);
        if (account == null) return LoginResult.of(LoginResultStatus.EMAIL_NOT_FOUND);

        String hash = account.getPasswordHash();
        if (hash == null) return LoginResult.of(LoginResultStatus.WRONG_PASSWORD);
        if (!(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"))) {
            return LoginResult.of(LoginResultStatus.WRONG_PASSWORD);
        }
        if (!passwordEncoder.matches(rawPassword, hash)) {
            return LoginResult.of(LoginResultStatus.WRONG_PASSWORD);
        }
        String status = account.getStatus() == null ? "" : account.getStatus().trim().toUpperCase();
        if (!"ACTIVE".equals(status)) return LoginResult.of(LoginResultStatus.INACTIVE_ACCOUNT);
        return LoginResult.success(account);
    }

    // =================================================== forgot / reset
    /**
     * Always returns true to avoid leaking whether an email is registered.
     * The OTP is only sent if the account exists AND is ACTIVE.
     * If sending the email fails we still return true – the side-effect of
     * asking for a reset must not depend on mail-server availability.
     */
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
        if (newPassword == null || newPassword.length() < 8
                || !PASSWORD_RE.matcher(newPassword).matches()) {
            return OtpResult.INVALID_OR_EXPIRED;
        }
        return verifyOtpAndConsume(email, code, OtpToken.PURPOSE_RESET_PASSWORD,
                accountId -> accountDAO.updatePasswordHash(accountId, passwordEncoder.encode(newPassword)));
    }

    // ======================================================= change password
    @Transactional
    public boolean changePassword(int accountId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 8
                || !PASSWORD_RE.matcher(newPassword).matches()) return false;
        Account acc = accountDAO.findById(accountId).orElse(null);
        if (acc == null) return false;
        if (!passwordEncoder.matches(oldPassword, acc.getPasswordHash())) return false;
        accountDAO.updatePasswordHash(accountId, passwordEncoder.encode(newPassword));
        return true;
    }

    // =========================================== OTP pre-check (no consume)
    /**
     * Validates an OTP WITHOUT consuming it – used by the "verify-reset-otp" page so the
     * user can move on to the password-reset form. The token is later consumed by
     * {@link #resetPassword(String, String, String)}.
     */
    @Transactional
    public OtpResult preCheckOtp(String email, String code, String purpose) {
        if (email == null || code == null) return OtpResult.INVALID_OR_EXPIRED;
        OtpToken t = otpTokenDAO.findActiveByEmailAndPurpose(email.trim(), purpose).orElse(null);
        if (t == null) return OtpResult.NO_PENDING_TOKEN;
        if (t.getAttemptCount() != null && t.getAttemptCount() >= maxOtpAttempts) {
            otpTokenDAO.markConsumed(t.getOtpTokenId());          // invalidate the token
            return OtpResult.TOO_MANY_ATTEMPTS;
        }
        if (!otpService.verify(code, t.getCodeHash())) {
            otpTokenDAO.incrementAttempt(t.getOtpTokenId());
            return OtpResult.INVALID_OR_EXPIRED;
        }
        return OtpResult.SUCCESS;
    }

    // =============================================================== helpers

    /**
     * Replaces any prior unconsumed token for the same email + purpose, creates
     * a new one and dispatches it by email. Returns OTP_DELIVERY_FAILED if the
     * mail-send step throws; in that case the token is still kept in DB so the
     * caller can re-trigger the email without creating a new code.
     */
    private RegisterResult dispatchOtp(Integer accountId, String email, String purpose) {
        otpTokenDAO.deleteAllByEmailAndPurpose(email, purpose);

        String code = otpService.generateCode();
        OtpToken t = new OtpToken();
        t.setAccountId(accountId);
        t.setEmail(email);
        t.setCodeHash(otpService.hash(code));
        t.setPurpose(purpose);
        t.setExpiresAt(LocalDateTime.now().plusSeconds(otpService.getOtpTtlSeconds()));
        otpTokenDAO.insert(t);

        if (!otpService.sendByEmail(email, code, purpose)) {
            return RegisterResult.OTP_DELIVERY_FAILED;
        }
        return RegisterResult.SUCCESS;
    }

    /**
     * Common OTP verification routine for both activation and password reset.
     * After a successful match the token is marked consumed and the side-effect
     * (activate account / change password) is executed.
     */
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
        if (t.getAccountId() != null) {
            sideEffect.apply(t.getAccountId());
        }
        return OtpResult.SUCCESS;
    }
}

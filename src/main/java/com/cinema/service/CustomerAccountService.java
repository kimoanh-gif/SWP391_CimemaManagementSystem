package com.cinema.service;

import com.cinema.dao.CustomerAccountDAO;
import com.cinema.model.CustomerAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for Customer Account Management (Admin UC-36).
 */
@Service
public class CustomerAccountService {

    private final CustomerAccountDAO dao;
    private final AuthService authService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public CustomerAccountService(CustomerAccountDAO dao, AuthService authService) {
        this.dao = dao;
        this.authService = authService;
    }

    // --- UC-36 Normal Flow ---

    public List<CustomerAccount> getAllCustomers() {
        return dao.findAllCustomers();
    }

    public List<CustomerAccount> searchCustomers(String keyword) {
        if (keyword == null || keyword.trim().isBlank()) {
            return getAllCustomers();
        }
        return dao.searchCustomers(keyword.trim());
    }

    /**
     * UC-36 Normal Flow: Paginated list with search.
     * @param page 1-based page number
     * @param size items per page (default 10)
     */
    public List<CustomerAccount> searchCustomers(String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isBlank()) {
            return dao.findAllCustomersPaginated(page, size);
        }
        return dao.searchCustomersPaginated(keyword.trim(), page, size);
    }

    /**
     * UC-36 Normal Flow: Total count for pagination.
     */
    public long countCustomers(String keyword) {
        if (keyword == null || keyword.trim().isBlank()) {
            return dao.countAll();
        }
        return dao.countBySearch(keyword.trim());
    }

    public Optional<CustomerAccount> getCustomerById(int accountId) {
        return dao.findById(accountId);
    }

    // --- Validation (E1, E2, E3, E4) ---

    public String validateAccount(String username, String email, String phone, Integer excludeId) {
        // ── E0: Required fields ──
        if (username == null || username.trim().isEmpty()) {
            return "EMPTY_USERNAME"; // handled separately
        }
        if (username.trim().length() < 3) {
            return "INVALID_USERNAME_LENGTH";
        }
        if (email == null || email.trim().isEmpty()) {
            return "EMPTY_EMAIL";
        }
        if (phone == null || phone.trim().isEmpty()) {
            return "EMPTY_PHONE";
        }

        // ── E4: Format validation ──
        String emailTrimmed = email.trim();
        if (!emailTrimmed.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$")) {
            return "INVALID_EMAIL"; // "Invalid email format."
        }
        String phoneTrimmed = phone.trim();
        if (!phoneTrimmed.matches("^0[0-9]{9,10}$")) {
            return "INVALID_PHONE_FORMAT"; // "Phone must be 10–11 digits starting with 0."
        }

        // ── E1/E2/E3: Uniqueness (trim & case-insensitive) ──
        if (dao.existsByUsername(username.trim(), excludeId)) {
            return "DUPLICATE_USERNAME"; // E1
        }
        if (dao.existsByEmail(emailTrimmed.toLowerCase(), excludeId)) {
            return "DUPLICATE_EMAIL"; // E2
        }
        if (dao.existsByPhone(phoneTrimmed, excludeId)) {
            return "DUPLICATE_PHONE"; // E3
        }
        return null; // Valid
    }

    // --- UC-36 A1: Create ---

    public OperationResult createAccount(String username, String email, String password,
                                          String fullName, String phone) {
        String validation = validateAccount(username, email, phone, null);
        if (validation != null) {
            return OperationResult.error(validation, getErrorMessage(validation));
        }
        if (password == null || password.length() < 6) {
            return OperationResult.error("INVALID_PASSWORD", "Password must be at least 6 characters.");
        }

        String hash = passwordEncoder.encode(password);
        int accountId = dao.insertAccount(username.trim(), email.trim().toLowerCase(),
                                           hash, fullName.trim(), phone.trim());
        return accountId > 0
                ? OperationResult.success("Account created successfully.")
                : OperationResult.error("CREATE_FAILED", "Failed to create account.");
    }

    // --- UC-36 A2: Update ---

    public OperationResult updateAccount(int accountId, String username, String email,
                                         String fullName, String phone) {
        Optional<CustomerAccount> existing = dao.findById(accountId);
        if (existing.isEmpty()) {
            return OperationResult.error("NOT_FOUND", "Account not found."); // E5
        }

        String validation = validateAccount(username, email, phone, accountId);
        if (validation != null) {
            return OperationResult.error(validation, getErrorMessage(validation));
        }

        int updated = dao.updateAccount(accountId, fullName.trim(), phone.trim(),
                                        email.trim().toLowerCase(), username.trim());
        return updated > 0
                ? OperationResult.success("Account updated successfully.")
                : OperationResult.error("UPDATE_FAILED", "Failed to update account.");
    }

    // --- UC-36 A3: Suspend ---

    public OperationResult suspendAccount(int accountId) {
        Optional<CustomerAccount> existing = dao.findById(accountId);
        if (existing.isEmpty()) {
            return OperationResult.error("NOT_FOUND", "Account not found."); // E5
        }
        if (!existing.get().isActive()) {
            return OperationResult.error("INVALID_STATUS", "Only active accounts can be suspended.");
        }

        int updated = dao.suspendAccount(accountId);
        return updated > 0
                ? OperationResult.success("Account suspended successfully.")
                : OperationResult.error("SUSPEND_FAILED", "Failed to suspend account.");
    }

    // --- UC-36 A4: Reactivate ---

    public OperationResult reactivateAccount(int accountId) {
        Optional<CustomerAccount> existing = dao.findById(accountId);
        if (existing.isEmpty()) {
            return OperationResult.error("NOT_FOUND", "Account not found."); // E5
        }
        if (existing.get().isActive()) {
            return OperationResult.error("INVALID_STATUS", "Account is already active.");
        }

        int updated = dao.reactivateAccount(accountId);
        return updated > 0
                ? OperationResult.success("Account reactivated successfully.")
                : OperationResult.error("REACTIVATE_FAILED", "Failed to reactivate account.");
    }

    // --- UC-36 A5: Reset Password (gửi OTP cho khách hàng) ---

    public OperationResult resetPassword(int accountId) {
        Optional<CustomerAccount> existing = dao.findById(accountId);
        if (existing.isEmpty()) {
            return OperationResult.error("NOT_FOUND", "Account not found."); // E5
        }
        CustomerAccount acc = existing.get();
        if (acc.getEmail() == null || acc.getEmail().isBlank()) {
            return OperationResult.error("NO_EMAIL", "Account has no email address.");
        }

        // Gửi OTP reset password qua email cho khách hàng
        authService.requestPasswordReset(acc.getEmail());
        return OperationResult.success("A password reset OTP has been sent to the customer's email.");
    }

    // --- Stats ---

    public long getTotalCount()    { return dao.countAll(); }
    public long getActiveCount()   { return dao.countByStatus("ACTIVE"); }
    public long getSuspendedCount(){ return dao.countByStatus("SUSPENDED"); }
    public long getPendingCount()  { return dao.countByStatus("PENDING_VERIFICATION"); }

    // --- Helpers ---

    private String getErrorMessage(String code) {
        return switch (code) {
            case "EMPTY_USERNAME"          -> "Username is required.";
            case "INVALID_USERNAME_LENGTH" -> "Username must be at least 3 characters.";
            case "EMPTY_EMAIL"             -> "Email is required.";
            case "INVALID_EMAIL"           -> "Invalid email format.";
            case "EMPTY_PHONE"             -> "Phone number is required.";
            case "INVALID_PHONE_FORMAT"    -> "Phone must be 10–11 digits starting with 0.";
            case "DUPLICATE_USERNAME"     -> "Username already exists. Please choose another.";
            case "DUPLICATE_EMAIL"         -> "Email already exists. Please use another email.";
            case "DUPLICATE_PHONE"         -> "Phone number already exists. Please use another.";
            case "INVALID_PASSWORD"       -> "Password must be at least 6 characters.";
            default                        -> "Validation error: " + code;
        };
    }

    /**
     * Simple result wrapper for operations.
     */
    public static class OperationResult {
        private final boolean success;
        private final String code;
        private final String message;

        private OperationResult(boolean success, String code, String message) {
            this.success = success;
            this.code = code;
            this.message = message;
        }

        public static OperationResult success(String message) {
            return new OperationResult(true, null, message);
        }

        public static OperationResult error(String code, String message) {
            return new OperationResult(false, code, message);
        }

        public boolean isSuccess()  { return success; }
        public String getCode()     { return code; }
        public String getMessage()  { return message; }
    }
}

package com.cinema.service;

import com.cinema.dao.AccountDAO;
import com.cinema.dao.RoleDAO;
import com.cinema.model.Account;
import com.cinema.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.security.SecureRandom;

/**
 * Service for Staff & Manager Account Management (Admin).
 */
@Service
public class StaffAccountService {

    private final AccountDAO accountDAO;
    private final RoleDAO roleDAO;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public StaffAccountService(AccountDAO accountDAO, RoleDAO roleDAO) {
        this.accountDAO = accountDAO;
        this.roleDAO = roleDAO;
    }

    public List<Account> getAllStaffs() {
        return accountDAO.findAll().stream()
                .filter(a -> !isCustomerRole(a.getRoleId()))
                .toList();
    }

    public List<Account> searchStaffs(String keyword) {
        if (keyword == null || keyword.trim().isBlank()) {
            return getAllStaffs();
        }
        return accountDAO.searchByKeyword(keyword.trim()).stream()
                .filter(a -> !isCustomerRole(a.getRoleId()))
                .toList();
    }

    public List<Account> searchStaffs(String keyword, int page, int size) {
        List<Account> all = searchStaffs(keyword);
        int start = (page - 1) * size;
        int end = Math.min(start + size, all.size());
        if (start >= all.size()) return List.of();
        return all.subList(start, end);
    }

    public long countStaffs(String keyword) {
        return searchStaffs(keyword).size();
    }

    public Optional<Account> getStaffById(int accountId) {
        return accountDAO.findById(accountId).filter(a -> !isCustomerRole(a.getRoleId()));
    }

    private boolean isCustomerRole(int roleId) {
        Optional<Role> role = roleDAO.findById(roleId);
        return role.map(r -> "CUSTOMER".equalsIgnoreCase(r.getName())).orElse(false);
    }

    public String validateAccount(String username, String email, String phone, Integer excludeId) {
        if (username == null || username.trim().isEmpty()) {
            return "EMPTY_USERNAME";
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

        String emailTrimmed = email.trim();
        if (!emailTrimmed.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$")) {
            return "INVALID_EMAIL";
        }
        String phoneTrimmed = phone.trim();
        if (!phoneTrimmed.matches("^0[0-9]{9,10}$")) {
            return "INVALID_PHONE_FORMAT";
        }

        if (accountDAO.existsByUsername(username.trim())) {
            return "DUPLICATE_USERNAME";
        }
        if (accountDAO.existsByEmail(emailTrimmed.toLowerCase())) {
            return "DUPLICATE_EMAIL";
        }
        return null;
    }

    public OperationResult createAccount(String username, String email, String password,
                                        String fullName, String phone, String roleName) {
        String validation = validateAccount(username, email, phone, null);
        if (validation != null) {
            return OperationResult.error(validation, getErrorMessage(validation));
        }
        if (password == null || password.length() < 6) {
            return OperationResult.error("INVALID_PASSWORD", "Password must be at least 6 characters.");
        }

        Optional<Role> role = roleDAO.findByName(roleName);
        if (role.isEmpty() || "CUSTOMER".equalsIgnoreCase(roleName)) {
            return OperationResult.error("INVALID_ROLE", "Invalid role selected.");
        }

        String hash = passwordEncoder.encode(password);
        Account account = new Account();
        account.setRoleId(role.get().getRoleId());
        account.setUsername(username.trim());
        account.setEmail(email.trim().toLowerCase());
        account.setPasswordHash(hash);
        account.setFullName(fullName.trim());
        account.setPhone(phone.trim());
        account.setStatus("ACTIVE");

        Integer id = accountDAO.insert(account);
        return id != null && id > 0
                ? OperationResult.success("Account created successfully.")
                : OperationResult.error("CREATE_FAILED", "Failed to create account.");
    }

    public OperationResult updateAccount(int accountId, String username, String email,
                                        String fullName, String phone, String roleName) {
        Optional<Account> existing = getStaffById(accountId);
        if (existing.isEmpty()) {
            return OperationResult.error("NOT_FOUND", "Account not found.");
        }

        if (email == null || email.trim().isEmpty()) {
            return OperationResult.error("EMPTY_EMAIL", "Email is required.");
        }
        if (phone == null || phone.trim().isEmpty()) {
            return OperationResult.error("EMPTY_PHONE", "Phone number is required.");
        }
        if (!email.trim().matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$")) {
            return OperationResult.error("INVALID_EMAIL", "Invalid email format.");
        }
        if (!phone.trim().matches("^0[0-9]{9,10}$")) {
            return OperationResult.error("INVALID_PHONE_FORMAT", "Phone must be 10-11 digits starting with 0.");
        }

        Optional<Role> role = roleDAO.findByName(roleName);
        if (role.isEmpty() || "CUSTOMER".equalsIgnoreCase(roleName)) {
            return OperationResult.error("INVALID_ROLE", "Invalid role selected.");
        }

        Account acc = existing.get();
        acc.setFullName(fullName.trim());
        acc.setPhone(phone.trim());
        acc.setEmail(email.trim().toLowerCase());
        acc.setRoleId(role.get().getRoleId());
        
        // Update role via DAO
        int updated = accountDAO.updateRole(accountId, role.get().getRoleId());
        int profileUpdated = accountDAO.updateProfile(accountId, fullName.trim(), phone.trim());
        
        return (updated > 0 || profileUpdated > 0)
                ? OperationResult.success("Account updated successfully.")
                : OperationResult.error("UPDATE_FAILED", "Failed to update account.");
    }

    public OperationResult suspendAccount(int accountId) {
        Optional<Account> existing = getStaffById(accountId);
        if (existing.isEmpty()) {
            return OperationResult.error("NOT_FOUND", "Account not found.");
        }
        if (!"ACTIVE".equals(existing.get().getStatus())) {
            return OperationResult.error("INVALID_STATUS", "Only active accounts can be suspended.");
        }

        int updated = accountDAO.updateStatus(accountId, "SUSPENDED");
        return updated > 0
                ? OperationResult.success("Account suspended successfully.")
                : OperationResult.error("SUSPEND_FAILED", "Failed to suspend account.");
    }

    public OperationResult reactivateAccount(int accountId) {
        Optional<Account> existing = getStaffById(accountId);
        if (existing.isEmpty()) {
            return OperationResult.error("NOT_FOUND", "Account not found.");
        }
        if ("ACTIVE".equals(existing.get().getStatus())) {
            return OperationResult.error("INVALID_STATUS", "Account is already active.");
        }

        int updated = accountDAO.updateStatus(accountId, "ACTIVE");
        return updated > 0
                ? OperationResult.success("Account reactivated successfully.")
                : OperationResult.error("REACTIVATE_FAILED", "Failed to reactivate account.");
    }

    /**
     * A5 - Reset Password
     * Generates a secure temporary password and returns it to the Admin.
     * Logic:
     * 1. Check account exists and is a staff/manager (not customer)
     * 2. Check account is not already suspended (suspended accounts can't reset password until reactivated)
     * 3. Generate cryptographically secure random password (12 chars: letters + digits)
     * 4. Hash and save to database
     * 5. Return plain temporary password for Admin to communicate to user
     */
    public OperationResult resetPassword(int accountId) {
        // 1. Check account exists
        Optional<Account> existing = getStaffById(accountId);
        if (existing.isEmpty()) {
            return OperationResult.error("NOT_FOUND", "Account not found.");
        }
        
        Account account = existing.get();
        
        // 2. Cannot reset password for suspended accounts - must reactivate first
        if ("SUSPENDED".equals(account.getStatus())) {
            return OperationResult.error("SUSPENDED_ACCOUNT", 
                    "Cannot reset password for suspended account. Please reactivate the account first.");
        }
        
        // 3. Generate secure temporary password
        String tempPassword = generateSecureTempPassword();
        
        // 4. Hash and save
        String hash = passwordEncoder.encode(tempPassword);
        int updated = accountDAO.updatePasswordHash(accountId, hash);
        
        if (updated <= 0) {
            return OperationResult.error("RESET_FAILED", "Failed to reset password. Please try again.");
        }
        
        // 5. Return success with temporary password
        return OperationResult.success(
                "Password has been reset successfully. Temporary password: " + tempPassword + 
                ". Please instruct the user to change this password after logging in.",
                tempPassword
        );
    }
    
    /**
     * Generates a cryptographically secure temporary password.
     * Format: 2 uppercase + 2 lowercase + 4 digits + 4 mixed = 12 chars
     * Example: Ma2bc9def3gh
     */
    private String generateSecureTempPassword() {
        String uppercase = "ABCDEFGHJKLMNPQRSTUVWXYZ";  // Removed I, O to avoid confusion
        String lowercase = "abcdefghjkmnpqrstuvwxyz";   // Removed l to avoid confusion  
        String digits = "23456789";                      // Removed 0, 1 to avoid confusion
        
        SecureRandom random = new SecureRandom();
        
        StringBuilder sb = new StringBuilder();
        // Add 2 uppercase
        for (int i = 0; i < 2; i++) {
            sb.append(uppercase.charAt(random.nextInt(uppercase.length())));
        }
        // Add 2 lowercase
        for (int i = 0; i < 2; i++) {
            sb.append(lowercase.charAt(random.nextInt(lowercase.length())));
        }
        // Add 4 digits
        for (int i = 0; i < 4; i++) {
            sb.append(digits.charAt(random.nextInt(digits.length())));
        }
        // Add 4 mixed chars
        String mixed = uppercase + lowercase + digits;
        for (int i = 0; i < 4; i++) {
            sb.append(mixed.charAt(random.nextInt(mixed.length())));
        }
        
        // Shuffle to mix characters
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        
        return new String(chars);
    }

    // Stats
    public long getTotalCount() { return getAllStaffs().size(); }
    
    public long getAdminCount() {
        return accountDAO.findAll().stream()
                .filter(a -> "ADMIN".equalsIgnoreCase(getRoleName(a.getRoleId())))
                .count();
    }
    
    public long getManagerCount() {
        return accountDAO.findAll().stream()
                .filter(a -> "MANAGER".equalsIgnoreCase(getRoleName(a.getRoleId())))
                .count();
    }
    
    public long getStaffCount() {
        return accountDAO.findAll().stream()
                .filter(a -> "STAFF".equalsIgnoreCase(getRoleName(a.getRoleId())))
                .count();
    }
    
    private String getRoleName(int roleId) {
        return roleDAO.findById(roleId).map(Role::getName).orElse("");
    }

    private String getErrorMessage(String code) {
        return switch (code) {
            case "EMPTY_USERNAME"          -> "Username is required.";
            case "INVALID_USERNAME_LENGTH" -> "Username must be at least 3 characters.";
            case "EMPTY_EMAIL"             -> "Email is required.";
            case "INVALID_EMAIL"           -> "Invalid email format.";
            case "EMPTY_PHONE"             -> "Phone number is required.";
            case "INVALID_PHONE_FORMAT"    -> "Phone must be 10-11 digits starting with 0.";
            case "DUPLICATE_USERNAME"      -> "Username already exists. Please choose another.";
            case "DUPLICATE_EMAIL"         -> "Email already exists. Please use another email.";
            case "DUPLICATE_PHONE"         -> "Phone number already exists. Please use another.";
            case "INVALID_PASSWORD"         -> "Password must be at least 6 characters.";
            case "INVALID_ROLE"             -> "Invalid role selected.";
            default                        -> "Validation error: " + code;
        };
    }

    public static class OperationResult {
        private final boolean success;
        private final String code;
        private final String message;
        private String tempPassword;  // For reset password feature

        private OperationResult(boolean success, String code, String message) {
            this.success = success;
            this.code = code;
            this.message = message;
        }
        
        private OperationResult(boolean success, String code, String message, String tempPassword) {
            this.success = success;
            this.code = code;
            this.message = message;
            this.tempPassword = tempPassword;
        }

        public static OperationResult success(String message) {
            return new OperationResult(true, null, message);
        }
        
        public static OperationResult success(String message, String tempPassword) {
            return new OperationResult(true, null, message, tempPassword);
        }

        public static OperationResult error(String code, String message) {
            return new OperationResult(false, code, message);
        }

        public boolean isSuccess()  { return success; }
        public String getCode()     { return code; }
        public String getMessage()  { return message; }
        public String getTempPassword() { return tempPassword; }
    }
}

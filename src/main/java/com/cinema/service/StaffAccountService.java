package com.cinema.service;

import com.cinema.dao.AccountDAO;
import com.cinema.dao.RoleDAO;
import com.cinema.model.Account;
import com.cinema.model.Role;
import com.cinema.util.AccountValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


/**
 * Service for Staff & Manager Account Management (Admin).
 */
@Service
public class StaffAccountService {

    private final AccountDAO accountDAO;
    private final RoleDAO roleDAO;
    private final AdminPasswordResetService passwordResetService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public StaffAccountService(AccountDAO accountDAO, RoleDAO roleDAO, AdminPasswordResetService passwordResetService) {
        this.accountDAO = accountDAO;
        this.roleDAO = roleDAO;
        this.passwordResetService = passwordResetService;
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
        if (!AccountValidation.USERNAME.matcher(username.trim()).matches()) {
            return "INVALID_USERNAME";
        }
        if (email == null || email.trim().isEmpty()) {
            return "EMPTY_EMAIL";
        }
        if (phone == null || phone.trim().isEmpty()) {
            return "EMPTY_PHONE";
        }

        String emailTrimmed = email.trim();
        if (!AccountValidation.EMAIL.matcher(emailTrimmed).matches()) {
            return "INVALID_EMAIL";
        }
        String phoneTrimmed = phone.trim();
        if (!AccountValidation.PHONE.matcher(phoneTrimmed).matches()) {
            return "INVALID_PHONE_FORMAT";
        }

        if (accountDAO.existsByUsername(username.trim())) {
            return "DUPLICATE_USERNAME";
        }
        if (accountDAO.existsByEmail(emailTrimmed.toLowerCase())) {
            return "DUPLICATE_EMAIL";
        }
        if (accountDAO.existsByPhone(phoneTrimmed)) {
            return "DUPLICATE_PHONE";
        }
        return null;
    }

    public OperationResult createAccount(String username, String email, String password,
                                        String fullName, String phone, String roleName) {
        if (!"STAFF".equals(roleName) && !"MANAGER".equals(roleName)) {
            return OperationResult.error("INVALID_ROLE", "Only Staff and Manager accounts can be created.");
        }
        if (fullName == null || fullName.trim().length() < 2 || fullName.trim().length() > 100) {
            return OperationResult.error("INVALID_FULLNAME", "Full name must be 2-100 characters.");
        }
        String validation = validateAccount(username, email, phone, null);
        if (validation != null) {
            return OperationResult.error(validation, getErrorMessage(validation));
        }
        if (password == null || !AccountValidation.PASSWORD.matcher(password).matches()) {
            return OperationResult.error("INVALID_PASSWORD", "Password must be 8-128 characters with at least one letter and one number.");
        }

        Optional<Role> role = roleDAO.findByName(roleName);
        if (role.isEmpty()) {
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
        if (!AccountValidation.EMAIL.matcher(email.trim()).matches()) {
            return OperationResult.error("INVALID_EMAIL", "Invalid email format.");
        }
        if (!AccountValidation.PHONE.matcher(phone.trim()).matches()) {
            return OperationResult.error("INVALID_PHONE_FORMAT", "Phone must start with 0 or +84 followed by 9-10 digits.");
        }
        if (fullName == null || fullName.trim().length() < 2 || fullName.trim().length() > 100) {
            return OperationResult.error("INVALID_FULLNAME", "Full name must be 2-100 characters.");
        }

        Optional<Role> role = roleDAO.findByName(roleName);
        if (role.isEmpty() || (!"STAFF".equals(roleName) && !"MANAGER".equals(roleName))) {
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

    /** Password plaintext is sent only to the account's registered address. */
    public OperationResult resetPassword(int accountId) {
        if (getStaffById(accountId).isEmpty()) return OperationResult.error("NOT_FOUND", "Account not found.");
        try {
            var result=passwordResetService.reset(accountId);
            return result.success() ? OperationResult.success(result.message())
                    : OperationResult.error(result.code(),result.message());
        } catch (AdminPasswordResetService.DeliveryFailure ex) {
            return OperationResult.error("EMAIL_FAILED", "Email delivery failed. The password was not changed. Please try again later.");
        } catch (org.springframework.dao.DataAccessException ex) {
            return OperationResult.error("RESET_FAILED", "Password reset failed. Please try again later.");
        }
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
            case "INVALID_USERNAME" -> "Username must be 4-50 characters using letters, numbers, dots, underscores or hyphens.";
            case "EMPTY_EMAIL"             -> "Email is required.";
            case "INVALID_EMAIL"           -> "Invalid email format.";
            case "EMPTY_PHONE"             -> "Phone number is required.";
            case "INVALID_PHONE_FORMAT"    -> "Phone must start with 0 or +84 followed by 9-10 digits.";
            case "DUPLICATE_USERNAME"      -> "Username already exists. Please choose another.";
            case "DUPLICATE_EMAIL"         -> "Email already exists. Please use another email.";
            case "DUPLICATE_PHONE"         -> "Phone number already exists. Please use another.";
            case "INVALID_PASSWORD"         -> "Password must be at least 6 characters.";
            case "INVALID_ROLE"             -> "Invalid role selected.";
            default                        -> "Validation error: " + code;
        };
    }

    public record OperationResult(boolean success, String code, String message) {
        public static OperationResult success(String message) { return new OperationResult(true,null,message); }
        public static OperationResult error(String code,String message) { return new OperationResult(false,code,message); }
        public boolean isSuccess() { return success; }
        public String getCode() { return code; }
        public String getMessage() { return message; }
    }
}

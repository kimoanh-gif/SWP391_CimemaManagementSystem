package com.cinema.service;

import com.cinema.dao.MembershipRankDAO;
import com.cinema.dao.SystemSettingDAO;
import com.cinema.model.MembershipRank;
import com.cinema.model.SystemSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for System Configuration Management.
 * Handles voucher settings, membership settings, and system rules.
 */
@Service
public class SystemConfigService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigService.class);

    private final SystemSettingDAO systemSettingDAO;
    private final MembershipRankDAO membershipRankDAO;

    @Autowired
    public SystemConfigService(SystemSettingDAO systemSettingDAO, MembershipRankDAO membershipRankDAO) {
        this.systemSettingDAO = systemSettingDAO;
        this.membershipRankDAO = membershipRankDAO;
    }

    @Override
    public void run(String... args) {
        try {
            systemSettingDAO.seedDefaultSettings();
            membershipRankDAO.seedDefaultRanks();
        } catch (Exception ex) {
            log.warn("Default configuration seeding was skipped because the database is unavailable: {}",
                    ex.getMessage());
        }
    }

    // ==================== SETTINGS ====================

    /**
     * Get all settings grouped by category.
     */
    public Map<String, List<SystemSetting>> getAllSettingsGrouped() {
        List<SystemSetting> all = systemSettingDAO.findActive();
        return all.stream()
                .collect(Collectors.groupingBy(SystemSetting::getCategory));
    }

    /**
     * Get all settings as flat list.
     */
    public List<SystemSetting> getAllSettings() {
        return systemSettingDAO.findActive();
    }

    /**
     * Get settings by category.
     */
    public List<SystemSetting> getSettingsByCategory(String category) {
        return systemSettingDAO.findByCategory(category);
    }

    /**
     * Get a single setting by key.
     */
    public Optional<SystemSetting> getSettingByKey(String key) {
        return systemSettingDAO.findByKey(key);
    }

    /**
     * Get setting value as string.
     */
    public String getSettingValue(String key) {
        return systemSettingDAO.findByKey(key)
                .map(SystemSetting::getValue)
                .orElse(null);
    }

    /**
     * Get setting value as integer.
     */
    public Integer getSettingValueAsInt(String key) {
        return systemSettingDAO.findByKey(key)
                .map(SystemSetting::getValueAsInt)
                .orElse(null);
    }

    // ==================== VALIDATION ====================

    /**
     * Validation result holder.
     */
    public record ValidationResult(boolean valid, String errorCode, String errorMessage) {
        public static ValidationResult success() { return new ValidationResult(true, null, null); }
        public static ValidationResult error(String code, String msg) { return new ValidationResult(false, code, msg); }
    }

    /**
     * Validate a setting value against its rules.
     * Exception E5: Invalid Configuration Value
     */
    public ValidationResult validateSetting(SystemSetting setting, String newValue) {
        if ("VOUCHER_MAX_DISCOUNT_AMOUNT".equals(setting.getSettingKey())
                && (newValue == null || !newValue.matches("[0-9]{1,12}"))) {
            return ValidationResult.error("INVALID_AMOUNT", "Enter a whole VND amount between 0 and 999999999999. Use 0 for no system-wide cap.");
        }
        String rules = setting.getValidationRules();
        if (rules == null || rules.isBlank()) {
            return ValidationResult.success(); // No rules, accept any value
        }

        // Required check
        if (rules.contains("required") && (newValue == null || newValue.isBlank())) {
            return ValidationResult.error("REQUIRED", "This field is required.");
        }

        // Read-only check
        if (rules.contains("readonly")) {
            return ValidationResult.error("READONLY", "This setting is read-only and cannot be modified.");
        }

        String type = setting.getValueType();

        // Type-specific validation
        switch (type) {
            case "NUMBER" -> {
                ValidationResult numResult = validateNumber(newValue, rules);
                if (!numResult.valid()) return numResult;
            }
            case "BOOLEAN" -> {
                if (newValue != null && !newValue.isBlank() && 
                    !newValue.equalsIgnoreCase("true") && !newValue.equalsIgnoreCase("false") &&
                    !newValue.equals("1") && !newValue.equals("0")) {
                    return ValidationResult.error("INVALID_BOOLEAN", "Value must be true or false.");
                }
            }
            case "DATE" -> {
                try {
                    LocalDateTime.parse(newValue);
                } catch (Exception e) {
                    return ValidationResult.error("INVALID_DATE", "Invalid date format. Use ISO format (yyyy-MM-ddTHH:mm).");
                }
            }
            // STRING type - no additional validation needed
        }

        return ValidationResult.success();
    }

    private ValidationResult validateNumber(String value, String rules) {
        if (value == null || value.isBlank()) {
            return ValidationResult.success(); // Empty is handled by required check
        }

        try {
            double numValue = Double.parseDouble(value);

            // Min check
            if (rules.contains("min:")) {
                int minIndex = rules.indexOf("min:") + 4;
                int minEnd = rules.indexOf(",", minIndex);
                if (minEnd == -1) minEnd = rules.length();
                double min = Double.parseDouble(rules.substring(minIndex, minEnd).trim());
                if (numValue < min) {
                    return ValidationResult.error("MIN_VALUE", "Value must be at least " + min + ".");
                }
            }

            // Max check
            if (rules.contains("max:")) {
                int maxIndex = rules.indexOf("max:") + 4;
                int maxEnd = rules.indexOf(",", maxIndex);
                if (maxEnd == -1) maxEnd = rules.length();
                double max = Double.parseDouble(rules.substring(maxIndex, maxEnd).trim());
                if (numValue > max) {
                    return ValidationResult.error("MAX_VALUE", "Value must not exceed " + max + ".");
                }
            }

        } catch (NumberFormatException e) {
            return ValidationResult.error("INVALID_NUMBER", "Value must be a valid number.");
        }

        return ValidationResult.success();
    }

    /**
     * Validate voucher validity dates.
     * Exception E1: Invalid Date Range
     */
    public ValidationResult validateVoucherValidity(String validFrom, String validTo) {
        if (validFrom == null || validFrom.isBlank()) {
            return ValidationResult.error("INVALID_DATE_RANGE", "Start date is required.");
        }
        if (validTo == null || validTo.isBlank()) {
            return ValidationResult.error("INVALID_DATE_RANGE", "End date is required.");
        }

        try {
            LocalDateTime from = LocalDateTime.parse(validFrom);
            LocalDateTime to = LocalDateTime.parse(validTo);
            
            if (to.isBefore(from)) {
                return ValidationResult.error("INVALID_DATE_RANGE", "End date must be after start date.");
            }
            if (from.isBefore(LocalDateTime.now())) {
                return ValidationResult.error("INVALID_DATE_RANGE", "Start date cannot be in the past.");
            }
        } catch (Exception e) {
            return ValidationResult.error("INVALID_DATE_RANGE", "Invalid date format. Use ISO format.");
        }

        return ValidationResult.success();
    }

    /**
     * Validate voucher quantity.
     * Exception E2: Invalid Quantity
     */
    public ValidationResult validateVoucherQuantity(int quantity) {
        if (quantity < 1) {
            return ValidationResult.error("INVALID_QUANTITY", "Quantity must be at least 1.");
        }
        if (quantity > 100000) {
            return ValidationResult.error("INVALID_QUANTITY", "Quantity cannot exceed 100,000.");
        }
        return ValidationResult.success();
    }

    /**
     * Validate voucher usage limit.
     * Exception E3: Invalid Usage Limit
     */
    public ValidationResult validateUsageLimit(int limit) {
        if (limit < 1) {
            return ValidationResult.error("INVALID_USAGE_LIMIT", "Usage limit must be at least 1.");
        }
        if (limit > 100) {
            return ValidationResult.error("INVALID_USAGE_LIMIT", "Usage limit cannot exceed 100 per voucher.");
        }
        return ValidationResult.success();
    }

    // ==================== UPDATE SETTINGS ====================

    /**
     * Update a setting value.
     * Normal Flow Step 8: System saves the updated value.
     */
    public OperationResult updateSetting(int settingId, String newValue) {
        // Get current setting
        Optional<SystemSetting> settingOpt = systemSettingDAO.findById(settingId);
        if (settingOpt.isEmpty()) {
            return OperationResult.error("NOT_FOUND", "Setting not found.");
        }

        SystemSetting setting = settingOpt.get();

        // Validate
        ValidationResult validation = validateSetting(setting, newValue);
        if (!validation.valid()) {
            return OperationResult.error(validation.errorCode(), validation.errorMessage());
        }

        // Update
        int updated = systemSettingDAO.updateValue(settingId, newValue);
        if (updated > 0) {
            return OperationResult.success(
                    "Setting '" + setting.getSettingName() + "' updated successfully.",
                    setting.getSettingKey(),
                    setting.getValue(), // old value
                    newValue            // new value
            );
        } else {
            return OperationResult.error("UPDATE_FAILED", "Failed to update setting.");
        }
    }

    /**
     * Batch update multiple settings.
     */
    public List<OperationResult> updateSettings(Map<Integer, String> settings) {
        List<OperationResult> results = new ArrayList<>();
        for (var entry : settings.entrySet()) {
            results.add(updateSetting(entry.getKey(), entry.getValue()));
        }
        return results;
    }

    /**
     * Reset a setting to its default value.
     */
    public OperationResult resetToDefault(int settingId) {
        Optional<SystemSetting> settingOpt = systemSettingDAO.findById(settingId);
        if (settingOpt.isEmpty()) {
            return OperationResult.error("NOT_FOUND", "Setting not found.");
        }

        SystemSetting setting = settingOpt.get();
        if (setting.getDefaultValue() == null) {
            return OperationResult.error("NO_DEFAULT", "This setting has no default value.");
        }

        return updateSetting(settingId, setting.getDefaultValue());
    }

    // ==================== MEMBERSHIP RANKS ====================

    /**
     * Get all membership ranks.
     */
    public List<MembershipRank> getAllRanks() {
        return membershipRankDAO.findAll();
    }

    /**
     * Get membership rank by ID.
     */
    public Optional<MembershipRank> getRankById(int rankId) {
        return membershipRankDAO.findById(rankId);
    }

    /**
     * Update membership rank threshold.
     * Alternative Flow A5: Configure Membership Rank Thresholds
     * Exception E4: Invalid Membership Threshold
     */
    @org.springframework.transaction.annotation.Transactional
    public OperationResult updateRankThreshold(int rankId, int newMinPoints) {
        membershipRankDAO.lockThresholds();
        Optional<MembershipRank> rankOpt = membershipRankDAO.findById(rankId);
        if (rankOpt.isEmpty()) {
            return OperationResult.error("NOT_FOUND", "Membership rank not found.");
        }

        MembershipRank rank = rankOpt.get();

        // Validate threshold
        ValidationResult validation = validateMembershipThreshold(rankId, newMinPoints);
        if (!validation.valid()) {
            return OperationResult.error(validation.errorCode(), validation.errorMessage());
        }

        // Update
        int updated = membershipRankDAO.updateThreshold(rankId, newMinPoints);
        if (updated > 0) {
            return OperationResult.success(
                    "Rank threshold for '" + rank.getDisplayName() + "' updated to " + newMinPoints + " points.",
                    rank.getName(),
                    String.valueOf(rank.getMinPoints()),
                    String.valueOf(newMinPoints)
            );
        } else {
            return OperationResult.error("UPDATE_FAILED", "Failed to update rank threshold.");
        }
    }

    /**
     * Validate membership rank threshold.
     * Ensures thresholds are in ascending order and don't overlap.
     * Exception E4: Invalid Membership Threshold
     */
    public ValidationResult validateMembershipThreshold(int rankId, int newMinPoints) {
        List<MembershipRank> allRanks = new ArrayList<>(membershipRankDAO.findAll());
        allRanks.sort(Comparator.comparingInt(MembershipRank::getMinPoints));

        // Find current rank position
        int currentIndex = -1;
        for (int i = 0; i < allRanks.size(); i++) {
            if (allRanks.get(i).getRankId() == rankId) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            return ValidationResult.error("NOT_FOUND", "Rank not found.");
        }

        MembershipRank current = allRanks.get(currentIndex);
        if (current.getMaxPoints() != null && newMinPoints > current.getMaxPoints()) {
            return ValidationResult.error("INVALID_THRESHOLD", "Minimum points cannot exceed this rank's maximum points.");
        }

        // Check lower bound (previous rank's max + 1)
        if (currentIndex > 0) {
            MembershipRank prevRank = allRanks.get(currentIndex - 1);
            if (prevRank.getMaxPoints() != null && newMinPoints <= prevRank.getMaxPoints()) {
                return ValidationResult.error("INVALID_THRESHOLD", "Minimum points must be greater than the previous rank's maximum (" + prevRank.getMaxPoints() + ").");
            }
            if (newMinPoints <= prevRank.getMinPoints()) {
                return ValidationResult.error("INVALID_THRESHOLD", 
                        "New threshold (" + newMinPoints + ") must be greater than " + 
                        prevRank.getDisplayName() + " threshold (" + prevRank.getMinPoints() + ").");
            }
        }

        // Check upper bound (next rank's min - 1)
        if (currentIndex < allRanks.size() - 1) {
            MembershipRank nextRank = allRanks.get(currentIndex + 1);
            if (nextRank.getMinPoints() != null && newMinPoints >= nextRank.getMinPoints()) {
                return ValidationResult.error("INVALID_THRESHOLD", 
                        "New threshold (" + newMinPoints + ") must be less than " + 
                        nextRank.getDisplayName() + " threshold (" + nextRank.getMinPoints() + ").");
            }
        }

        // Non-negative check
        if (newMinPoints < 0) {
            return ValidationResult.error("INVALID_THRESHOLD", "Threshold cannot be negative.");
        }

        return ValidationResult.success();
    }

    /**
     * Update membership rank benefits.
     * Alternative Flow A6: Configure Membership Benefits
     */
    public OperationResult updateRankBenefits(int rankId, String benefitsJson) {
        Optional<MembershipRank> rankOpt = membershipRankDAO.findById(rankId);
        if (rankOpt.isEmpty()) {
            return OperationResult.error("NOT_FOUND", "Membership rank not found.");
        }

        MembershipRank rank = rankOpt.get();
        rank.setBenefits(benefitsJson);
        
        int updated = membershipRankDAO.update(rank);
        if (updated > 0) {
            return OperationResult.success("Benefits for '" + rank.getDisplayName() + "' updated successfully.");
        } else {
            return OperationResult.error("UPDATE_FAILED", "Failed to update benefits.");
        }
    }

    // ==================== RESULT CLASSES ====================

    public record OperationResult(
            boolean success, 
            String code, 
            String message,
            String settingKey,
            String oldValue,
            String newValue
    ) {
        public static OperationResult success(String message) {
            return new OperationResult(true, null, message, null, null, null);
        }
        public static OperationResult success(String message, String key, String oldVal, String newVal) {
            return new OperationResult(true, null, message, key, oldVal, newVal);
        }
        public static OperationResult error(String code, String message) {
            return new OperationResult(false, code, message, null, null, null);
        }
    }
}

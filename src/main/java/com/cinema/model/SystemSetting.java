package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * System Setting model for storing system configuration.
 * Supports different value types: STRING, NUMBER, BOOLEAN, DATE, JSON
 */
public class SystemSetting {

    private Integer settingId;
    private String settingKey;     // Unique key (e.g., "VOUCHER_VALIDITY_DAYS")
    private String settingName;    // Display name (e.g., "Voucher Validity Days")
    private String category;       // Category: VOUCHER, MEMBERSHIP, BOOKING, SYSTEM
    private String valueType;      // Type: STRING, NUMBER, BOOLEAN, DATE, JSON
    private String value;          // Current value
    private String defaultValue;   // Default value
    private String description;    // Description for Admin
    private String validationRules; // Validation rules (e.g., "min:1,max:365")
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SystemSetting() {}

    // ==================== VALUE GETTERS WITH TYPE CONVERSION ====================

    public Integer getValueAsInt() {
        if (value == null || value.isBlank()) return null;
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return null; }
    }

    public Long getValueAsLong() {
        if (value == null || value.isBlank()) return null;
        try { return Long.parseLong(value); } catch (NumberFormatException e) { return null; }
    }

    public BigDecimal getValueAsBigDecimal() {
        if (value == null || value.isBlank()) return null;
        try { return new BigDecimal(value); } catch (NumberFormatException e) { return null; }
    }

    public Boolean getValueAsBoolean() {
        if (value == null || value.isBlank()) return null;
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    public LocalDateTime getValueAsDateTime() {
        if (value == null || value.isBlank()) return null;
        try { return LocalDateTime.parse(value); } catch (Exception e) { return null; }
    }

    // ==================== GETTERS & SETTERS ====================

    public Integer getSettingId() { return settingId; }
    public void setSettingId(Integer settingId) { this.settingId = settingId; }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }

    public String getSettingName() { return settingName; }
    public void setSettingName(String settingName) { this.settingName = settingName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getValidationRules() { return validationRules; }
    public void setValidationRules(String validationRules) { this.validationRules = validationRules; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ==================== HELPER METHODS ====================

    public boolean isRequired() {
        return validationRules != null && validationRules.contains("required");
    }

    public boolean isReadOnly() {
        return validationRules != null && validationRules.contains("readonly");
    }

    @Override
    public String toString() {
        return "SystemSetting{" +
                "settingKey='" + settingKey + '\'' +
                ", settingName='" + settingName + '\'' +
                ", category='" + category + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}

package com.cinema.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Form DTO for TicketPricing rule creation and updating (Screen 60).
 */
public class TicketPricingForm {

    private Integer pricingId;

    @NotBlank(message = "Pricing rule code cannot be empty")
    @Size(max = 50, message = "Pricing rule code cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,50}$", message = "Pricing rule code must contain only letters, numbers, hyphens or underscores (2-50 characters)")
    private String pricingCode;

    @NotBlank(message = "Pricing rule name cannot be empty")
    @Size(max = 150, message = "Pricing rule name cannot exceed 150 characters")
    private String ruleName;

    @NotBlank(message = "Applicable day type cannot be empty")
    @Pattern(regexp = "^(?i)(WEEKDAY|WEEKEND|HOLIDAY)$", message = "Invalid day type (WEEKDAY, WEEKEND, HOLIDAY)")
    private String dayType; // 'WEEKDAY', 'WEEKEND', 'HOLIDAY'

    @NotBlank(message = "Time slot cannot be empty")
    @Pattern(regexp = "^(?i)(MORNING|STANDARD|GOLDEN_HOUR|ALL_DAY)$", message = "Invalid time slot (MORNING, STANDARD, GOLDEN_HOUR, ALL_DAY)")
    private String timeSlot; // 'MORNING', 'STANDARD', 'GOLDEN_HOUR', 'ALL_DAY'

    @NotNull(message = "Base ticket price cannot be empty")
    @DecimalMin(value = "0.0", message = "Base ticket price cannot be negative")
    @DecimalMax(value = "10000000.0", message = "Base ticket price exceeds allowed limit")
    private BigDecimal basePrice;

    @NotNull(message = "Format surcharge cannot be empty")
    @DecimalMin(value = "0.0", message = "Format surcharge cannot be negative")
    @DecimalMax(value = "10000000.0", message = "Format surcharge exceeds allowed limit")
    private BigDecimal formatSurcharge = BigDecimal.ZERO;

    @NotBlank(message = "Experience format cannot be empty")
    @Size(max = 50, message = "Experience format cannot exceed 50 characters")
    private String experienceType = "2D Digital";

    @Pattern(regexp = "^(?i)(Active|Inactive)$", message = "Rule status must be Active or Inactive")
    private String status = "Active";

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    public TicketPricingForm() {
    }

    public Integer getPricingId() {
        return pricingId;
    }

    public void setPricingId(Integer pricingId) {
        this.pricingId = pricingId;
    }

    public String getPricingCode() {
        return pricingCode;
    }

    public void setPricingCode(String pricingCode) {
        this.pricingCode = pricingCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getDayType() {
        return dayType;
    }

    public void setDayType(String dayType) {
        this.dayType = dayType;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getFormatSurcharge() {
        return formatSurcharge;
    }

    public void setFormatSurcharge(BigDecimal formatSurcharge) {
        this.formatSurcharge = formatSurcharge;
    }

    public String getExperienceType() {
        return experienceType;
    }

    public void setExperienceType(String experienceType) {
        this.experienceType = experienceType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

package com.cinema.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Represents a Ticket Pricing configuration rule (Screen 60 / UC-31 / BR-50 / BR-51).
 */
public class TicketPricing {

    private Integer pricingId;
    private String pricingCode;
    private String ruleName;
    private String dayType;          // 'WEEKDAY', 'WEEKEND', 'HOLIDAY'
    private String timeSlot;         // 'MORNING', 'STANDARD', 'GOLDEN_HOUR', 'ALL_DAY'
    private BigDecimal basePrice;    // Standard 2D base ticket price
    private BigDecimal formatSurcharge; // e.g. 0, 20000 (3D), 60000 (IMAX)
    private String experienceType;   // '2D Digital', '3D RealD', 'Dolby Atmos', 'IMAX Laser'
    private String status;           // 'Active', 'Inactive'
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TicketPricing() {
    }

    public TicketPricing(Integer pricingId, String pricingCode, String ruleName, String dayType,
                         String timeSlot, BigDecimal basePrice, BigDecimal formatSurcharge,
                         String experienceType, String status, String description) {
        this.pricingId = pricingId;
        this.pricingCode = pricingCode;
        this.ruleName = ruleName;
        this.dayType = dayType;
        this.timeSlot = timeSlot;
        this.basePrice = basePrice;
        this.formatSurcharge = formatSurcharge;
        this.experienceType = experienceType;
        this.status = status;
        this.description = description;
    }

    /**
     * Calculates the calculated ticket unit price according to BR-50:
     * Ticket unit price = (Showtime base ticket price + Format Surcharge) * SeatType price multiplier,
     * rounded to the nearest 1,000 VND.
     *
     * @param multiplier SeatType price multiplier (e.g. 1.0, 1.25, 1.8)
     * @return Unit price rounded to nearest 1,000 VND.
     */
    public BigDecimal calculateTicketPrice(BigDecimal multiplier) {
        if (multiplier == null || multiplier.compareTo(BigDecimal.ZERO) <= 0) {
            multiplier = BigDecimal.ONE;
        }
        BigDecimal effectiveBase = (basePrice != null ? basePrice : BigDecimal.ZERO)
                .add(formatSurcharge != null ? formatSurcharge : BigDecimal.ZERO);
        BigDecimal rawPrice = effectiveBase.multiply(multiplier);
        // Round to nearest 1,000 VND (BR-50)
        BigDecimal thousand = new BigDecimal("1000");
        return rawPrice.divide(thousand, 0, RoundingMode.HALF_UP).multiply(thousand);
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isActive() {
        return "Active".equalsIgnoreCase(this.status);
    }
}

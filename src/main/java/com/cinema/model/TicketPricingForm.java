package com.cinema.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Form DTO for TicketPricing rule creation and updating (Screen 60).
 */
public class TicketPricingForm {

    private Integer pricingId;

    @NotBlank(message = "Mã quy tắc định giá không được để trống")
    @Size(max = 50, message = "Mã quy tắc tối đa 50 ký tự")
    private String pricingCode;

    @NotBlank(message = "Tên quy tắc định giá không được để trống")
    @Size(max = 150, message = "Tên quy tắc tối đa 150 ký tự")
    private String ruleName;

    @NotBlank(message = "Loại ngày áp dụng không được để trống")
    private String dayType; // 'WEEKDAY', 'WEEKEND', 'HOLIDAY'

    @NotBlank(message = "Khung giờ chiếu không được để trống")
    private String timeSlot; // 'MORNING', 'STANDARD', 'GOLDEN_HOUR', 'ALL_DAY'

    @NotNull(message = "Giá vé cơ sở không được để trống")
    @DecimalMin(value = "0.0", message = "Giá vé cơ sở không được là số âm")
    private BigDecimal basePrice;

    @NotNull(message = "Phụ thu định dạng không được để trống")
    @DecimalMin(value = "0.0", message = "Phụ thu định dạng không được là số âm")
    private BigDecimal formatSurcharge = BigDecimal.ZERO;

    @NotBlank(message = "Định dạng trải nghiệm không được để trống")
    private String experienceType = "2D Digital";

    private String status = "Active";

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
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

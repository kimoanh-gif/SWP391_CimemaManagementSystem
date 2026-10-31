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

    @NotBlank(message = "Mã quy tắc định giá không được để trống")
    @Size(max = 50, message = "Mã quy tắc tối đa 50 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,50}$", message = "Mã quy tắc chỉ gồm chữ, số, dấu gạch ngang hoặc gạch dưới (2-50 ký tự)")
    private String pricingCode;

    @NotBlank(message = "Tên quy tắc định giá không được để trống")
    @Size(max = 150, message = "Tên quy tắc tối đa 150 ký tự")
    private String ruleName;

    @NotBlank(message = "Loại ngày áp dụng không được để trống")
    @Pattern(regexp = "^(?i)(WEEKDAY|WEEKEND|HOLIDAY)$", message = "Loại ngày áp dụng không hợp lệ (WEEKDAY, WEEKEND, HOLIDAY)")
    private String dayType; // 'WEEKDAY', 'WEEKEND', 'HOLIDAY'

    @NotBlank(message = "Khung giờ chiếu không được để trống")
    @Pattern(regexp = "^(?i)(MORNING|STANDARD|GOLDEN_HOUR|ALL_DAY)$", message = "Khung giờ chiếu không hợp lệ (MORNING, STANDARD, GOLDEN_HOUR, ALL_DAY)")
    private String timeSlot; // 'MORNING', 'STANDARD', 'GOLDEN_HOUR', 'ALL_DAY'

    @NotNull(message = "Giá vé cơ sở không được để trống")
    @DecimalMin(value = "0.0", message = "Giá vé cơ sở không được là số âm")
    @DecimalMax(value = "10000000.0", message = "Giá vé cơ sở vượt quá giới hạn cho phép")
    private BigDecimal basePrice;

    @NotNull(message = "Phụ thu định dạng không được để trống")
    @DecimalMin(value = "0.0", message = "Phụ thu định dạng không được là số âm")
    @DecimalMax(value = "10000000.0", message = "Phụ thu định dạng vượt quá giới hạn cho phép")
    private BigDecimal formatSurcharge = BigDecimal.ZERO;

    @NotBlank(message = "Định dạng trải nghiệm không được để trống")
    @Size(max = 50, message = "Định dạng trải nghiệm tối đa 50 ký tự")
    private String experienceType = "2D Digital";

    @Pattern(regexp = "^(?i)(Active|Inactive)$", message = "Trạng thái quy tắc chỉ nhận giá trị Active hoặc Inactive")
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

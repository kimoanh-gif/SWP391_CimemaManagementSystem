package com.cinema.dto;

import com.cinema.model.ManagerVoucherRow;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Form for M11.1 / M11.3 when the type is "Voucher / Coupon".
 * Validation is done in ManagerVoucherService.
 */
public class VoucherForm {

    private Integer promotionId;
    private String code;
    /** PERCENT or FIXED. */
    private String discountType = "PERCENT";
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minBookingAmount;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime validFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime validTo;
    private Integer totalUsageLimit;
    private Integer perAccountLimit;
    private Integer requiredRankId;
    private String status = "ACTIVE";

    public static VoucherForm from(ManagerVoucherRow row) {
        VoucherForm f = new VoucherForm();
        f.promotionId = row.getPromotionId();
        f.code = row.getCode();
        f.discountType = row.isPercentage() ? "PERCENT" : "FIXED";
        f.discountValue = row.getDiscountValue();
        f.maxDiscountAmount = row.getMaxDiscountAmount();
        f.minBookingAmount = row.getMinBookingAmount();
        f.validFrom = row.getValidFrom();
        f.validTo = row.getValidTo();
        f.totalUsageLimit = row.getTotalUsageLimit();
        f.perAccountLimit = row.getPerAccountLimit();
        f.requiredRankId = row.getRequiredRankId();
        f.status = row.getStatus();
        return f;
    }

    public String getValidFromInput() { return PromotionForm.toInput(validFrom); }
    public String getValidToInput() { return PromotionForm.toInput(validTo); }

    /** Number without trailing zeros for number inputs (45000.00 -> 45000). */
    public static String plain(BigDecimal value) {
        return value != null ? value.stripTrailingZeros().toPlainString() : "";
    }

    public String getDiscountValueInput() { return plain(discountValue); }
    public String getMaxDiscountInput() { return plain(maxDiscountAmount); }
    public String getMinBookingInput() { return plain(minBookingAmount); }

    public Integer getPromotionId() { return promotionId; }
    public void setPromotionId(Integer promotionId) { this.promotionId = promotionId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }
    public BigDecimal getMinBookingAmount() { return minBookingAmount; }
    public void setMinBookingAmount(BigDecimal minBookingAmount) { this.minBookingAmount = minBookingAmount; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }
    public Integer getTotalUsageLimit() { return totalUsageLimit; }
    public void setTotalUsageLimit(Integer totalUsageLimit) { this.totalUsageLimit = totalUsageLimit; }
    public Integer getPerAccountLimit() { return perAccountLimit; }
    public void setPerAccountLimit(Integer perAccountLimit) { this.perAccountLimit = perAccountLimit; }
    public Integer getRequiredRankId() { return requiredRankId; }
    public void setRequiredRankId(Integer requiredRankId) { this.requiredRankId = requiredRankId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

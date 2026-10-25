package com.cinema.model;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One Voucher on the Manager Promotion &amp; Voucher screens (M11).
 */
public class ManagerVoucherRow {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int voucherId;
    private int promotionId;
    private String promotionName;
    private String promotionStatus;
    private LocalDateTime promotionStart;
    private LocalDateTime promotionEnd;
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minBookingAmount;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer totalUsageLimit;
    private Integer perAccountLimit;
    private Integer requiredRankId;
    private String requiredRankName;
    private String status;
    private int usageCount;
    private int referenceCount;
    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal discountTotal = BigDecimal.ZERO;

    public int getVoucherId() { return voucherId; }
    public void setVoucherId(int voucherId) { this.voucherId = voucherId; }
    public int getPromotionId() { return promotionId; }
    public void setPromotionId(int promotionId) { this.promotionId = promotionId; }
    public String getPromotionName() { return promotionName; }
    public void setPromotionName(String promotionName) { this.promotionName = promotionName; }
    public String getPromotionStatus() { return promotionStatus; }
    public void setPromotionStatus(String promotionStatus) { this.promotionStatus = promotionStatus; }
    public LocalDateTime getPromotionStart() { return promotionStart; }
    public void setPromotionStart(LocalDateTime promotionStart) { this.promotionStart = promotionStart; }
    public LocalDateTime getPromotionEnd() { return promotionEnd; }
    public void setPromotionEnd(LocalDateTime promotionEnd) { this.promotionEnd = promotionEnd; }
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
    public String getRequiredRankName() { return requiredRankName; }
    public void setRequiredRankName(String requiredRankName) { this.requiredRankName = requiredRankName; }
    public String getStatus() { return status != null ? status.trim().toUpperCase() : ""; }
    public void setStatus(String status) { this.status = status; }
    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
    public int getReferenceCount() { return referenceCount; }
    public void setReferenceCount(int referenceCount) { this.referenceCount = referenceCount; }
    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue != null ? revenue : BigDecimal.ZERO; }
    public BigDecimal getDiscountTotal() { return discountTotal; }
    public void setDiscountTotal(BigDecimal discountTotal) { this.discountTotal = discountTotal != null ? discountTotal : BigDecimal.ZERO; }

    // ------------------------------------------------------------------ display helpers

    public boolean isPercentage() { return VoucherDiscounts.isPercentage(discountType); }

    public String getTypeLabel() { return isPercentage() ? "Percentage" : "Fixed amount"; }

    public String getValueDisplay() { return ManagerPromotionRow.valueDisplay(discountType, discountValue); }

    public String getMaxDiscountDisplay() {
        return maxDiscountAmount != null && maxDiscountAmount.signum() > 0
                ? new DecimalFormat("#,###").format(maxDiscountAmount) + " ₫" : "No cap";
    }

    public String getMinBookingDisplay() {
        return minBookingAmount != null && minBookingAmount.signum() > 0
                ? new DecimalFormat("#,###").format(minBookingAmount) + " ₫" : "No minimum";
    }

    public String getTotalLimitDisplay() { return totalUsageLimit != null ? totalUsageLimit + " uses" : "Unlimited"; }

    public String getPerAccountDisplay() { return perAccountLimit != null ? perAccountLimit + " per customer" : "Unlimited"; }

    public String getRankDisplay() { return requiredRankName != null ? requiredRankName + " or higher" : "All members"; }

    /** A voucher is only usable while its promotion is also active, so the stricter status wins. */
    public String getDisplayStatus() {
        LocalDateTime now = LocalDateTime.now();
        String own = PromotionLifecycle.displayStatus(status, validFrom, validTo, now);
        String parent = PromotionLifecycle.displayStatus(promotionStatus, promotionStart, promotionEnd, now);
        if (PromotionLifecycle.ACTIVE.equals(own) && !PromotionLifecycle.ACTIVE.equals(parent)) {
            return parent;
        }
        return own;
    }

    public String getDisplayStatusLabel() { return PromotionLifecycle.label(getDisplayStatus()); }

    public String getTimeHint() {
        return PromotionLifecycle.timeHint(getDisplayStatus(), validFrom, validTo, LocalDateTime.now());
    }

    public String getValidityDisplay() {
        return (validFrom != null ? validFrom.format(DATE) : "") + " - " + (validTo != null ? validTo.format(DATE) : "");
    }

    public String getValidFromDisplay() { return validFrom != null ? validFrom.format(DATE_TIME) : ""; }
    public String getValidToDisplay() { return validTo != null ? validTo.format(DATE_TIME) : ""; }

    public boolean isLocked() { return referenceCount > 0; }

    public boolean isStopped() { return PromotionLifecycle.INACTIVE.equals(getStatus()); }
}

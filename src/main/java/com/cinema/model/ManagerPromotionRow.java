package com.cinema.model;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One Promotion (campaign) on the Manager Promotion &amp; Voucher screens (M11).
 */
public class ManagerPromotionRow {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int promotionId;
    private String name;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private int voucherCount;
    private String primaryCode;
    private String primaryDiscountType;
    private BigDecimal primaryDiscountValue;
    private int usageCount;
    private int referenceCount;
    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal discountTotal = BigDecimal.ZERO;

    public int getPromotionId() { return promotionId; }
    public void setPromotionId(int promotionId) { this.promotionId = promotionId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public String getStatus() { return status != null ? status.trim().toUpperCase() : ""; }
    public void setStatus(String status) { this.status = status; }
    public int getVoucherCount() { return voucherCount; }
    public void setVoucherCount(int voucherCount) { this.voucherCount = voucherCount; }
    public String getPrimaryCode() { return primaryCode; }
    public void setPrimaryCode(String primaryCode) { this.primaryCode = primaryCode; }
    public String getPrimaryDiscountType() { return primaryDiscountType; }
    public void setPrimaryDiscountType(String primaryDiscountType) { this.primaryDiscountType = primaryDiscountType; }
    public BigDecimal getPrimaryDiscountValue() { return primaryDiscountValue; }
    public void setPrimaryDiscountValue(BigDecimal primaryDiscountValue) { this.primaryDiscountValue = primaryDiscountValue; }
    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
    public int getReferenceCount() { return referenceCount; }
    public void setReferenceCount(int referenceCount) { this.referenceCount = referenceCount; }
    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue != null ? revenue : BigDecimal.ZERO; }
    public BigDecimal getDiscountTotal() { return discountTotal; }
    public void setDiscountTotal(BigDecimal discountTotal) { this.discountTotal = discountTotal != null ? discountTotal : BigDecimal.ZERO; }

    // ------------------------------------------------------------------ display helpers

    public String getCode() { return String.format("#PRM-%03d", promotionId); }

    public String getDisplayStatus() {
        return PromotionLifecycle.displayStatus(status, startDate, endDate, LocalDateTime.now());
    }

    public String getDisplayStatusLabel() { return PromotionLifecycle.label(getDisplayStatus()); }

    public String getTimeHint() {
        return PromotionLifecycle.timeHint(getDisplayStatus(), startDate, endDate, LocalDateTime.now());
    }

    public String getPeriodDisplay() {
        return (startDate != null ? startDate.format(DATE) : "") + " - " + (endDate != null ? endDate.format(DATE) : "");
    }

    public String getStartDisplay() { return startDate != null ? startDate.format(DATE_TIME) : ""; }
    public String getEndDisplay() { return endDate != null ? endDate.format(DATE_TIME) : ""; }

    /** "10%", "50,000 ₫" or "No voucher yet". */
    public String getPrimaryValueDisplay() {
        if (primaryCode == null) return "No voucher yet";
        return valueDisplay(primaryDiscountType, primaryDiscountValue);
    }

    public String getPrimaryTypeLabel() {
        if (primaryCode == null) return "—";
        return VoucherDiscounts.isPercentage(primaryDiscountType) ? "Percentage" : "Fixed amount";
    }

    /** "1 code", "1,840 bookings" – number with thousands separator and singular/plural word. */
    public static String count(long n, String singular, String plural) {
        return String.format(java.util.Locale.US, "%,d %s", n, n == 1 ? singular : plural);
    }

    public String getVoucherCountText() { return count(voucherCount, "code", "codes"); }
    public String getVoucherCodesText() { return count(voucherCount, "voucher code", "voucher codes"); }
    public String getMoreCodesText() { return "+" + count(voucherCount - 1L, "more code", "more codes"); }
    public String getBookingCountText() { return count(referenceCount, "booking", "bookings"); }

    /** Audit rule from the mockup: anything already used in a booking cannot be hard-deleted. */
    public boolean isLocked() { return referenceCount > 0; }

    public boolean isStopped() { return PromotionLifecycle.INACTIVE.equals(getStatus()); }

    static String valueDisplay(String type, BigDecimal value) {
        if (value == null) return "";
        if (VoucherDiscounts.isPercentage(type)) return value.stripTrailingZeros().toPlainString() + "%";
        return new DecimalFormat("#,###").format(value) + " ₫";
    }
}

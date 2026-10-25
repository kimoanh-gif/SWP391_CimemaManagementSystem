package com.cinema.model;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model representing a Voucher tied to a Promotion.
 */
public class Voucher {

    private Integer voucherId;
    private Integer promotionId;
    private Integer requiredRankId;
    private String code;
    private String discountType; // PERCENTAGE, FIXED_AMOUNT, AMOUNT
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minBookingAmount;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer totalUsageLimit;
    private Integer perAccountLimit;
    private String status;

    public Voucher() {
    }

    public Integer getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Integer voucherId) {
        this.voucherId = voucherId;
    }

    public Integer getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(Integer promotionId) {
        this.promotionId = promotionId;
    }

    public Integer getRequiredRankId() {
        return requiredRankId;
    }

    public void setRequiredRankId(Integer requiredRankId) {
        this.requiredRankId = requiredRankId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public BigDecimal getMinBookingAmount() {
        return minBookingAmount;
    }

    public void setMinBookingAmount(BigDecimal minBookingAmount) {
        this.minBookingAmount = minBookingAmount;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public Integer getTotalUsageLimit() {
        return totalUsageLimit;
    }

    public void setTotalUsageLimit(Integer totalUsageLimit) {
        this.totalUsageLimit = totalUsageLimit;
    }

    public Integer getPerAccountLimit() {
        return perAccountLimit;
    }

    public void setPerAccountLimit(Integer perAccountLimit) {
        this.perAccountLimit = perAccountLimit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDiscountDisplay() {
        if (discountValue == null) return "Giảm giá đặc biệt";
        if (VoucherDiscounts.isPercentage(discountType)) {
            return "Giảm " + discountValue.stripTrailingZeros().toPlainString() + "%";
        }
        DecimalFormat df = new DecimalFormat("###,###");
        return "Giảm " + df.format(discountValue) + " đ";
    }

    public String getFormattedValidRange() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String from = validFrom != null ? validFrom.format(fmt) : "";
        String to = validTo != null ? validTo.format(fmt) : "Vô thời hạn";
        return from + " - " + to;
    }
}

package com.cinema.model;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One voucher row on the Voucher / Promotion Selection screen.
 * Status values follow the SRS: AVAILABLE, APPLIED, INVALID, EXPIRED.
 */
public class VoucherOption {

    public static final String AVAILABLE = "AVAILABLE";
    public static final String APPLIED = "APPLIED";
    public static final String INVALID = "INVALID";
    public static final String EXPIRED = "EXPIRED";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VoucherCandidate voucher;
    private String status;
    private String reason;
    private BigDecimal discountAmount = BigDecimal.ZERO;

    public VoucherOption(VoucherCandidate voucher) {
        this.voucher = voucher;
    }

    public int getVoucherId() { return voucher.voucherId(); }
    public String getCode() { return voucher.code(); }
    public String getPromotionName() { return voucher.promotionName(); }
    public String getDescription() { return voucher.promotionDescription(); }
    public BigDecimal getMinBookingAmount() { return voucher.minBookingAmount(); }
    public String getRequiredRankName() { return voucher.requiredRankName(); }
    public LocalDateTime getValidFrom() { return voucher.validFrom(); }
    public LocalDateTime getValidTo() { return voucher.validTo(); }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
    }

    public boolean isSelectable() {
        return AVAILABLE.equals(status) || APPLIED.equals(status);
    }

    public boolean isApplied() {
        return APPLIED.equals(status);
    }

    /** "10%" (max 50,000 ₫) or "30,000 ₫". */
    public String getDiscountValueDisplay() {
        DecimalFormat money = new DecimalFormat("#,###");
        if (voucher.discountValue() == null) return "";
        if (voucher.isPercentage()) {
            String text = voucher.discountValue().stripTrailingZeros().toPlainString() + "%";
            if (voucher.maxDiscountAmount() != null && voucher.maxDiscountAmount().signum() > 0) {
                text += " (max " + money.format(voucher.maxDiscountAmount()) + " ₫)";
            }
            return text;
        }
        return money.format(voucher.discountValue()) + " ₫";
    }

    public String getValidityDisplay() {
        String from = voucher.validFrom() != null ? voucher.validFrom().format(DATE) : "";
        String to = voucher.validTo() != null ? voucher.validTo().format(DATE) : "";
        return from + " - " + to;
    }
}

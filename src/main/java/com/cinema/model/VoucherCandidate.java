package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Voucher row joined with its Promotion and required MembershipRank,
 * used to validate a voucher for a Booking (BR-17..21, BR-38).
 */
public record VoucherCandidate(
        int voucherId,
        String code,
        String discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minBookingAmount,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        Integer totalUsageLimit,
        Integer perAccountLimit,
        String status,
        String promotionName,
        String promotionDescription,
        String promotionStatus,
        LocalDateTime promotionStart,
        LocalDateTime promotionEnd,
        Integer requiredRankId,
        String requiredRankName,
        Integer requiredRankMinPoints
) {

    public boolean isPercentage() {
        return VoucherDiscounts.isPercentage(discountType);
    }
}

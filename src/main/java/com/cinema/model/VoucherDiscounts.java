package com.cinema.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Discount calculation shared by the voucher screens.
 * DiscountType values found in the database: PERCENT, PERCENTAGE, %, FIXED, FIXED_AMOUNT, AMOUNT.
 */
public final class VoucherDiscounts {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private VoucherDiscounts() {
    }

    public static boolean isPercentage(String discountType) {
        if (discountType == null) return false;
        String type = discountType.trim().toUpperCase();
        return type.equals("PERCENT") || type.equals("PERCENTAGE") || type.equals("%");
    }

    /**
     * Discount for a booking subtotal:
     * percentage = subtotal × value / 100, capped by MaxDiscountAmount;
     * fixed      = value.
     * The result is rounded to whole VND and never exceeds the subtotal.
     */
    public static BigDecimal calculate(String discountType, BigDecimal discountValue,
                                       BigDecimal maxDiscountAmount, BigDecimal subtotal) {
        if (discountValue == null || subtotal == null || subtotal.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount;
        if (isPercentage(discountType)) {
            discount = subtotal.multiply(discountValue).divide(HUNDRED, 0, RoundingMode.HALF_UP);
            if (maxDiscountAmount != null && maxDiscountAmount.signum() > 0
                    && discount.compareTo(maxDiscountAmount) > 0) {
                discount = maxDiscountAmount;
            }
        } else {
            discount = discountValue;
        }
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        return discount.max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP);
    }
}

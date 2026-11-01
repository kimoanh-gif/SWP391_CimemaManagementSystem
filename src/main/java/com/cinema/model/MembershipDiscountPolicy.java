package com.cinema.model;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record MembershipDiscountPolicy(int usageLimit, BigDecimal minimumOrder, BigDecimal cap, BigDecimal percent) {
    public BigDecimal discount(BigDecimal subtotal, int used) {
        if (subtotal == null || subtotal.signum() <= 0 || usageLimit <= 0 || used >= usageLimit
                || minimumOrder == null || subtotal.compareTo(minimumOrder) < 0 || cap == null || cap.signum() <= 0
                || percent == null || percent.signum() <= 0 || percent.compareTo(BigDecimal.valueOf(100)) > 0)
            return BigDecimal.ZERO;
        return subtotal.multiply(percent).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                .min(cap).min(subtotal).setScale(0, RoundingMode.DOWN);
    }
}

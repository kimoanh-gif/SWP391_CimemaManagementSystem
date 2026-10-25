package com.cinema.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * KPI cards of M11: running campaigns, live vouchers, revenue from discounted bookings, ending soon.
 */
public record ManagerPromotionStats(int runningPromotions, int totalPromotions,
                                   int liveVouchers, int totalVouchers, int voucherUses,
                                   BigDecimal revenueThisMonth, BigDecimal revenueLastMonth,
                                   BigDecimal discountThisMonth,
                                   int endingSoon, List<String> endingSoonNames) {

    public static ManagerPromotionStats empty() {
        return new ManagerPromotionStats(0, 0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, List.of());
    }

    /** Change vs last month in %, or null when last month had no revenue. */
    public Integer getRevenueChangePercent() {
        if (revenueLastMonth == null || revenueLastMonth.signum() == 0) return null;
        return revenueThisMonth.subtract(revenueLastMonth)
                .multiply(BigDecimal.valueOf(100))
                .divide(revenueLastMonth, 0, java.math.RoundingMode.HALF_UP)
                .intValue();
    }

    public String getEndingSoonText() {
        return endingSoonNames == null || endingSoonNames.isEmpty() ? "None in the next 7 days" : String.join(", ", endingSoonNames);
    }
}

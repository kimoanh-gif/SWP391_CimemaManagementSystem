package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AvailableVoucher(
        Integer voucherId,
        String code,
        String promotionName,
        String description,
        String discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minBookingAmount,
        LocalDateTime validTo
) {
}

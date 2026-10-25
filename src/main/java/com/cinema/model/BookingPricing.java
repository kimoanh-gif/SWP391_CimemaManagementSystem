package com.cinema.model;

import java.math.BigDecimal;

/**
 * Money and voucher state of one Booking, read for the voucher step.
 */
public record BookingPricing(
        int bookingId,
        int customerId,
        int tierPoints,
        String status,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        Integer voucherId,
        String voucherCode
) {

    public boolean hasVoucher() {
        return voucherId != null;
    }

    public boolean isEditable() {
        return "PENDING".equalsIgnoreCase(status);
    }
}

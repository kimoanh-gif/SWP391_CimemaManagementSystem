package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StaffBookingListItem(
        Integer bookingId,
        String bookingCode,
        String customerName,
        String phone,
        String movieTitle,
        LocalDateTime showtime,
        String bookingType,
        String status,
        BigDecimal totalAmount
) {
}

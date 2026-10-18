package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StaffBookingDetail(
        Integer bookingId,
        String bookingCode,
        String bookingType,
        String status,
        LocalDateTime bookingTime,
        String customerName,
        String phone,
        String email,
        String movieTitle,
        String roomName,
        LocalDateTime showtime,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        List<SeatLine> seats,
        List<FoodLine> foods
) {
    public record SeatLine(String label, String seatType, BigDecimal price, String status) {}
    public record FoodLine(String name, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
}

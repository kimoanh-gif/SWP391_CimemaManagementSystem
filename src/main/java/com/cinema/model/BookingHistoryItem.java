package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingHistoryItem(int bookingId, String bookingCode, String movieTitle,
                                 String roomName, LocalDateTime startTime, String seatLabels,
                                 BigDecimal totalAmount, String bookingStatus, String paymentStatus) {
}

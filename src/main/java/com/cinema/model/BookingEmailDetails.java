package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingEmailDetails(int bookingId, String bookingCode, String email, String customerName,
                                  String movieTitle, String roomName, LocalDateTime startTime,
                                  String seats, String foodAndDrinks, BigDecimal totalAmount) {}

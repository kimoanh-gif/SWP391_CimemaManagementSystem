package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentView(int paymentId, int bookingId, String bookingCode,
                          String transactionRef, String method, BigDecimal amount,
                          String status, LocalDateTime expiresAt, LocalDateTime heldUntil) {
}

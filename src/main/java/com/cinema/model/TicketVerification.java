package com.cinema.model;

import java.time.LocalDateTime;

public record TicketVerification(int ticketId, String qrCode, String ticketStatus,
                                 String bookingCode, String customerName, String email,
                                 String phone, String movieTitle, String roomName,
                                 LocalDateTime startTime, String seatLabel) {
}

package com.cinema.model;

import java.time.LocalDateTime;

public record CustomerTicket(int ticketId, String qrCode, String ticketStatus,
                             String bookingCode, String movieTitle, String roomName,
                             LocalDateTime startTime, String seatLabel) {
}

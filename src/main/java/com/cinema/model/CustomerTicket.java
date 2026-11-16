package com.cinema.model;

import java.time.LocalDateTime;

public record CustomerTicket(int ticketId, String qrCode, String ticketStatus,
                             String bookingCode, String movieTitle, String roomName,
                             LocalDateTime startTime, String seatLabel, String posterUrl) {
    /** Human-readable code accepted by the staff check-in screen. */
    public String ticketCode() {
        return String.format("TK-%08d", ticketId);
    }
}

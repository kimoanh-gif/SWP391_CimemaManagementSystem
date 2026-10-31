package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Data for the Ticket & Receipt Printing screen (SRS UC24, screen 5.5). */
public record StaffBookingPrint(
        Integer bookingId,
        String bookingCode,
        String bookingType,
        String status,
        String movieTitle,
        String roomName,
        LocalDateTime showtime,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String paymentMethod,
        BigDecimal paidAmount,
        LocalDateTime paidAt,
        String cashierName,
        List<TicketLine> tickets,
        List<StaffBookingDetail.FoodLine> foods
) {
    public record TicketLine(int ticketId, String qrCode, String seatLabel, String seatType,
                             BigDecimal price, String ticketStatus) {
        /** Printed ticket ID, e.g. TK-00000123. */
        public String ticketCode() {
            return String.format("TK-%08d", ticketId);
        }
    }

    public boolean printable() {
        return "CONFIRMED".equalsIgnoreCase(status) && !tickets.isEmpty();
    }
}

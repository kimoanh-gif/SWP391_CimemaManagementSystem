package com.cinema.model;

import java.time.LocalDateTime;

/**
 * Outcome of verifying one ticket at the entrance (SRS UC21, screen 5.6). ageNotice is set when the
 * movie's age rating needs an ID check before entry (UC22, screen 5.7).
 */
public record StaffTicketCheck(
        Result result,
        String message,
        Ticket ticket,
        String ageNotice
) {
    public StaffTicketCheck(Result result, String message, Ticket ticket) {
        this(result, message, ticket, null);
    }

    public enum Result {
        /** Valid, paid, unused ticket inside the check-in window. */
        VALID,
        /** Checked in just now by Confirm Check-in. */
        CHECKED_IN,
        /** BR-27 anti-passback: the ticket was already checked in. */
        ALREADY_USED,
        /** BR-28: outside the check-in window of its showtime. */
        WRONG_SHOWTIME,
        /** BR-26: booking not paid or ticket cancelled. */
        INVALID,
        NOT_FOUND,
        /** UC22 A1/E1: Staff denied entry after the age check; the ticket stays unused. */
        AGE_DENIED
    }

    /** Why Staff refused an age-restricted ticket (UC22 A1 and E1). */
    public enum AgeDenialReason {
        UNDERAGE("Customer does not meet the age requirement"),
        NO_ID("Customer refused or could not show a valid ID");

        private final String label;

        AgeDenialReason(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public boolean ageCheckRequired() {
        return ageNotice != null;
    }

    public record Ticket(int ticketId, String qrCode, String ticketStatus, LocalDateTime checkedInAt,
                         String bookingCode, String bookingStatus, String movieTitle, String ageRating,
                         String roomName, String seatLabel, String seatType,
                         LocalDateTime startTime, LocalDateTime endTime) {
        /** Printed ticket ID, e.g. TK-00000123. */
        public String ticketCode() {
            return String.format("TK-%08d", ticketId);
        }
    }

    public boolean admissible() {
        return result == Result.VALID;
    }
}

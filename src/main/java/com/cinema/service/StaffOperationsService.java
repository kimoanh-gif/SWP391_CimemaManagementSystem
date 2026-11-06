package com.cinema.service;

import com.cinema.dao.StaffNewsDAO;
import com.cinema.dao.StaffOperationsDAO;
import com.cinema.model.StaffBookingDetail;
import com.cinema.model.StaffBookingPage;
import com.cinema.model.StaffBookingPrint;
import com.cinema.model.StaffDashboardData;
import com.cinema.model.StaffTicketCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StaffOperationsService {

    public static final int PAGE_SIZE = 20;
    /** Check-in opens this many minutes before the showtime and closes when it ends. */
    static final int CHECK_IN_OPENS_MINUTES = 30;
    private static final String TICKET_VERIFY_PATH = "/ticket/verify/";
    private static final Pattern TICKET_ID = Pattern.compile("(?i)(?:TK-?)?0*(\\d{1,10})");
    private static final DateTimeFormatter CHECK_IN_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final Pattern MINIMUM_AGE = Pattern.compile("T(\\d{1,2})");
    private static final Logger log = LoggerFactory.getLogger(StaffOperationsService.class);

    private final StaffNewsDAO staffNewsDAO;
    private final StaffOperationsDAO staffOperationsDAO;
    private final Clock clock;

    @Autowired
    public StaffOperationsService(StaffNewsDAO staffNewsDAO, StaffOperationsDAO staffOperationsDAO) {
        this(staffNewsDAO, staffOperationsDAO, Clock.systemDefaultZone());
    }

    StaffOperationsService(StaffNewsDAO staffNewsDAO, StaffOperationsDAO staffOperationsDAO, Clock clock) {
        this.staffNewsDAO = staffNewsDAO;
        this.staffOperationsDAO = staffOperationsDAO;
        this.clock = clock;
    }

    public StaffDashboardData dashboard(int accountId, String role) {
        boolean allStaff = isPrivileged(role);
        Integer staffId = allStaff ? null : requireStaffId(accountId);
        return staffOperationsDAO.getDashboardData(staffId, allStaff);
    }

    public StaffBookingPage bookings(int accountId, String role, int requestedPage,
                                     String query, String requestedStatus, LocalDate showDate) {
        requireBookingViewer(accountId, role);
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        String status = requestedStatus == null || requestedStatus.isBlank()
                || "ALL".equalsIgnoreCase(requestedStatus)
                ? null : requestedStatus.trim().toUpperCase(Locale.ROOT);
        if (status != null && !java.util.Set.of("PENDING", "PAYMENT_PENDING", "CONFIRMED",
                "CANCELLED", "EXPIRED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid booking status filter.");
        }
        long totalItems = staffOperationsDAO.countBookings(normalizedQuery, status, showDate);
        int totalPages = (int) Math.max(1, (totalItems + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, totalPages - 1));
        return new StaffBookingPage(staffOperationsDAO.findBookings(normalizedQuery, status, showDate,
                page * PAGE_SIZE, PAGE_SIZE), page, totalPages, totalItems, normalizedQuery, status, showDate);
    }

    public StaffBookingDetail bookingDetail(int accountId, String role, int bookingId) {
        requireBookingViewer(accountId, role);
        return staffOperationsDAO.findBookingDetail(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Booking was not found."));
    }

    /** Tickets and receipt for printing (UC24). Only a paid booking has valid tickets to print (BR-32). */
    public StaffBookingPrint printData(int accountId, String role, int bookingId) {
        requireBookingViewer(accountId, role);
        StaffBookingPrint print = staffOperationsDAO.findBookingPrint(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Booking was not found."));
        if (!print.printable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only confirmed (paid) bookings have tickets to print.");
        }
        return print;
    }

    // Staff can view all bookings, online ones included, but only with an active staff profile.
    private void requireBookingViewer(int accountId, String role) {
        if (!isPrivileged(role)) requireStaffId(accountId);
    }

    /** UC21 steps 3-6: validates a scanned or typed ticket without changing it. */
    public StaffTicketCheck verifyTicket(String code) {
        return evaluate(findTicket(code));
    }

    /**
     * UC21 step 7 (Confirm Check-in): Valid -> Checked-In with the check-in timestamp (BR-36).
     * An age-restricted ticket also needs Staff to confirm the customer's age (UC22 step 5).
     */
    public StaffTicketCheck confirmCheckIn(String code, boolean ageConfirmed) {
        StaffTicketCheck check = verifyTicket(code);
        if (!check.admissible()) return check;
        if (check.ageCheckRequired() && !ageConfirmed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirm the customer's age for the "
                    + check.ticket().ageRating().trim() + " rating before check-in.");
        }
        if (staffOperationsDAO.checkInTicket(check.ticket().ticketId()) == 0) {
            // Another gate admitted it, or the window closed, between verify and confirm.
            return evaluate(findTicket(code));
        }
        StaffTicketCheck.Ticket ticket = staffOperationsDAO
                .findTicketForCheckIn(check.ticket().qrCode(), null).orElse(check.ticket());
        return new StaffTicketCheck(StaffTicketCheck.Result.CHECKED_IN,
                "Ticket Validated Successfully. Customer checked in.", ticket);
    }

    /** UC22 A1/E1 (Deny Entry): refuses an age-restricted ticket and logs why. The ticket stays unused. */
    public StaffTicketCheck denyEntry(String code, String reason, int accountId) {
        StaffTicketCheck check = verifyTicket(code);
        if (!check.admissible() || !check.ageCheckRequired()) return check;
        StaffTicketCheck.AgeDenialReason denial;
        try {
            denial = StaffTicketCheck.AgeDenialReason.valueOf(reason == null ? "" : reason.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose why entry is denied.");
        }
        StaffTicketCheck.Ticket ticket = check.ticket();
        log.warn("Age check denied entry: ticket {} ({}, rated {}) booking {}, staff account {}, reason {}",
                ticket.ticketCode(), ticket.movieTitle(), ticket.ageRating(), ticket.bookingCode(), accountId, denial);
        return new StaffTicketCheck(StaffTicketCheck.Result.AGE_DENIED,
                "Entry denied: " + denial.label() + ". The ticket was not checked in.", ticket, check.ageNotice());
    }

    /** Banner text for a rating that needs an ID check (BR-40), or null when the movie is open to all ages. */
    static String ageNotice(String ageRating) {
        if (!StaffCounterSalesService.requiresAgeCheck(ageRating)) return null;
        String rating = ageRating.trim().toUpperCase(Locale.ROOT);
        Matcher minimumAge = MINIMUM_AGE.matcher(rating);
        if (minimumAge.matches()) {
            return rating + " - Restricted to persons aged " + minimumAge.group(1) + " and above";
        }
        if ("K".equals(rating)) return "K - Viewers under 13 must be accompanied by a parent or guardian";
        return rating + " - Age-restricted movie";
    }

    private Optional<StaffTicketCheck.Ticket> findTicket(String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scan a ticket QR code or enter a ticket ID.");
        }
        String value = code.trim();
        // A scanned QR holds the e-ticket link .../ticket/verify/CINEORA-...; keep only the code.
        int verifyPath = value.indexOf(TICKET_VERIFY_PATH);
        if (verifyPath >= 0) value = value.substring(verifyPath + TICKET_VERIFY_PATH.length()).split("[?#/]")[0];
        Matcher ticketId = TICKET_ID.matcher(value);
        if (ticketId.matches()) {
            try {
                return staffOperationsDAO.findTicketForCheckIn(null, Integer.parseInt(ticketId.group(1)));
            } catch (NumberFormatException tooLong) {
                return Optional.empty();
            }
        }
        return staffOperationsDAO.findTicketForCheckIn(value, null);
    }

    private StaffTicketCheck evaluate(Optional<StaffTicketCheck.Ticket> found) {
        if (found.isEmpty()) {
            return new StaffTicketCheck(StaffTicketCheck.Result.NOT_FOUND,
                    "Ticket not found. Check the code and try again.", null);
        }
        StaffTicketCheck.Ticket ticket = found.get();
        if ("USED".equalsIgnoreCase(ticket.ticketStatus())) {
            String when = ticket.checkedInAt() == null ? "" : " at " + ticket.checkedInAt().format(CHECK_IN_TIME);
            return new StaffTicketCheck(StaffTicketCheck.Result.ALREADY_USED,
                    "This ticket has already been used" + when + ". Entry denied.", ticket);
        }
        if (!"VALID".equalsIgnoreCase(ticket.ticketStatus()) || !"CONFIRMED".equalsIgnoreCase(ticket.bookingStatus())) {
            return new StaffTicketCheck(StaffTicketCheck.Result.INVALID,
                    "This ticket is not valid (booking not paid or ticket cancelled). Entry denied.", ticket);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime opensAt = ticket.startTime().minusMinutes(CHECK_IN_OPENS_MINUTES);
        if (now.isBefore(opensAt) || now.isAfter(ticket.endTime())) {
            String detail = now.isBefore(opensAt)
                    ? "Check-in opens at " + opensAt.format(CHECK_IN_TIME) + "."
                    : "This showtime has already ended.";
            return new StaffTicketCheck(StaffTicketCheck.Result.WRONG_SHOWTIME,
                    "Ticket invalid for current showtime or room. " + detail + " Entry denied.", ticket);
        }
        String ageNotice = ageNotice(ticket.ageRating());
        return new StaffTicketCheck(StaffTicketCheck.Result.VALID, ageNotice == null
                ? "Ticket Validated Successfully."
                : "Ticket Validated Successfully. Check the customer's ID before entry.", ticket, ageNotice);
    }

    private int requireStaffId(int accountId) {
        return staffNewsDAO.findActiveStaffId(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "An active staff profile is required."));
    }

    private static boolean isPrivileged(String role) {
        return "ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role);
    }
}

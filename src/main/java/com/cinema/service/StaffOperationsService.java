package com.cinema.service;

import com.cinema.dao.StaffNewsDAO;
import com.cinema.dao.StaffOperationsDAO;
import com.cinema.model.StaffBookingDetail;
import com.cinema.model.StaffBookingPage;
import com.cinema.model.StaffDashboardData;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Locale;

@Service
public class StaffOperationsService {

    public static final int PAGE_SIZE = 20;

    private final StaffNewsDAO staffNewsDAO;
    private final StaffOperationsDAO staffOperationsDAO;

    public StaffOperationsService(StaffNewsDAO staffNewsDAO, StaffOperationsDAO staffOperationsDAO) {
        this.staffNewsDAO = staffNewsDAO;
        this.staffOperationsDAO = staffOperationsDAO;
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

    // Staff can view all bookings, online ones included, but only with an active staff profile.
    private void requireBookingViewer(int accountId, String role) {
        if (!isPrivileged(role)) requireStaffId(accountId);
    }

    public int checkIn(String bookingCode) {
        if (bookingCode == null || bookingCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a booking code.");
        }
        bookingCode = bookingCode.trim();
        int checkedIn = staffOperationsDAO.checkInBooking(bookingCode);
        if (checkedIn > 0) return checkedIn;
        if (!staffOperationsDAO.hasBooking(bookingCode)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking code was not found.");
        }
        int alreadyCheckedIn = staffOperationsDAO.countCheckInTickets(bookingCode);
        if (alreadyCheckedIn > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tickets for this booking were already checked in.");
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Check-in is available only for confirmed bookings during the showtime check-in window.");
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

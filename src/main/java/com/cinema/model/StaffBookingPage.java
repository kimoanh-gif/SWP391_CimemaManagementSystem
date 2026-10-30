package com.cinema.model;

import java.time.LocalDate;
import java.util.List;

public record StaffBookingPage(
        List<StaffBookingListItem> bookings,
        int page,
        int totalPages,
        long totalItems,
        String query,
        String status,
        LocalDate showDate
) {
}

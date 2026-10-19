package com.cinema.model;

import java.util.List;

public record StaffDashboardData(
        int counterTicketsSoldToday,
        int pendingCounterOrders,
        List<PublicShowtime> upcomingShowtimes
) {
}

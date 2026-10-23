package com.cinema.controller;

import com.cinema.model.AuthenticatedUser;
import com.cinema.model.DashboardRevenueDay;
import com.cinema.model.ManagerDashboardStats;
import com.cinema.service.ManagerDashboardInsightsService;
import com.cinema.service.ManagerDashboardService;
import com.cinema.service.SessionService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Controller for Screen 53 / mockup M01: Manager Dashboard (UC-25 / SWP391 Iteration 2).
 * The primary operations hub and navigation center for cinema managers.
 */
@Controller
public class ManagerDashboardController {

    private static final int RECENT_BOOKINGS_LIMIT = 5;

    private final ManagerDashboardService dashboardService;
    private final ManagerDashboardInsightsService insightsService;
    private final SessionService sessionService;

    public ManagerDashboardController(ManagerDashboardService dashboardService,
                                      ManagerDashboardInsightsService insightsService,
                                      SessionService sessionService) {
        this.dashboardService = dashboardService;
        this.insightsService = insightsService;
        this.sessionService = sessionService;
    }

    @GetMapping({"/manager", "/manager/dashboard"})
    public String showDashboard(Model model, HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        ManagerDashboardStats stats = dashboardService.getDashboardStats();
        model.addAttribute("stats", stats);
        model.addAttribute("movieCount", stats.getTotalMovies());
        model.addAttribute("roomCount", stats.getActiveRooms());
        model.addAttribute("upcomingShowtimeCount", stats.getTodayShowtimesCount());
        model.addAttribute("todayShowtimes", dashboardService.getTodayShowtimes());
        model.addAttribute("activeMovies", dashboardService.getActiveMovies());
        model.addAttribute("rooms", dashboardService.getRoomsSummary());
        model.addAttribute("userRole", ManagerSecurityUtils.resolveRole(request));

        // Revenue overview chart (last 7 days) + recent bookings table
        List<DashboardRevenueDay> weeklyRevenue = insightsService.getWeeklyRevenue();
        BigDecimal weeklyTicket = weeklyRevenue.stream().map(DashboardRevenueDay::getTicketRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal weeklyFb = weeklyRevenue.stream().map(DashboardRevenueDay::getFbRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal weeklyTotal = weeklyTicket.add(weeklyFb);
        int ticketShare = weeklyTotal.signum() > 0
                ? weeklyTicket.multiply(BigDecimal.valueOf(100)).divide(weeklyTotal, 0, java.math.RoundingMode.HALF_UP).intValue()
                : 0;
        BigDecimal todayRevenue = weeklyRevenue.stream().filter(DashboardRevenueDay::isToday)
                .map(DashboardRevenueDay::getTotalRevenue).findFirst().orElse(BigDecimal.ZERO);

        model.addAttribute("weeklyRevenue", weeklyRevenue);
        model.addAttribute("weeklyRevenueTotal", weeklyTotal);
        model.addAttribute("ticketSharePercent", ticketShare);
        model.addAttribute("fbSharePercent", weeklyTotal.signum() > 0 ? 100 - ticketShare : 0);
        model.addAttribute("todayRevenue", todayRevenue);
        model.addAttribute("recentBookings", insightsService.getRecentBookings(RECENT_BOOKINGS_LIMIT));

        HttpSession session = request.getSession(false);
        AuthenticatedUser user = session == null ? null : sessionService.getCurrentUser(session);
        if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) {
            model.addAttribute("userName", user.getFullName());
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale.ENGLISH);
        model.addAttribute("currentDateStr", LocalDate.now().format(dateFormatter));

        return "manager/dashboard";
    }
}

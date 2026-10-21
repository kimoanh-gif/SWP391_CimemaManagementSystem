package com.cinema.controller;

import com.cinema.model.ManagerDashboardStats;
import com.cinema.service.ManagerDashboardService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Controller for Screen 53: Manager Dashboard (UC-25 / SWP391 Iteration 2).
 * The primary operations hub and navigation center for cinema managers.
 */
@Controller
public class ManagerDashboardController {

    private final ManagerDashboardService dashboardService;

    public ManagerDashboardController(ManagerDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/manager", "/manager/dashboard"})
    public String showDashboard(
            @org.springframework.web.bind.annotation.RequestParam(value = "period", required = false, defaultValue = "last7days") String period,
            Model model,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        ManagerDashboardStats stats = (period == null || "last7days".equalsIgnoreCase(period))
                ? dashboardService.getDashboardStats()
                : dashboardService.getDashboardStats(period);
        if (stats == null) {
            stats = new ManagerDashboardStats();
        }
        if (period != null && !period.isBlank()) {
            stats.setSelectedPeriod(period);
        }

        model.addAttribute("stats", stats);
        model.addAttribute("selectedPeriod", stats.getSelectedPeriod());
        model.addAttribute("movieCount", stats.getTotalMovies());
        model.addAttribute("roomCount", stats.getActiveRooms());
        model.addAttribute("upcomingShowtimeCount", stats.getTodayShowtimesCount());
        model.addAttribute("todayShowtimes", dashboardService.getTodayShowtimes());
        model.addAttribute("upcomingShowtimes", dashboardService.getUpcomingShowtimes());
        model.addAttribute("activeMovies", dashboardService.getActiveMovies());
        model.addAttribute("rooms", dashboardService.getRoomsSummary());
        String resolvedRole = ManagerSecurityUtils.resolveRole(request);
        model.addAttribute("userRole", resolvedRole != null ? resolvedRole : "MANAGER");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy", Locale.ENGLISH);
        model.addAttribute("currentDateStr", LocalDate.now().format(dateFormatter));

        return "manager/dashboard";
    }
}

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



        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, 'ngày' dd/MM/yyyy", new Locale("vi", "VN"));
        model.addAttribute("currentDateStr", LocalDate.now().format(dateFormatter));

        return "manager/dashboard";
    }
}

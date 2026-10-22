package com.cinema.controller;

import com.cinema.service.ManagerDashboardService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** M01 - Manager dashboard. */
@Controller
public class ManagerDashboardController {

    private final ManagerDashboardService dashboardService;
    private final SessionService sessionService;

    public ManagerDashboardController(ManagerDashboardService dashboardService, SessionService sessionService) {
        this.dashboardService = dashboardService;
        this.sessionService = sessionService;
    }

    @GetMapping("/manager/dashboard")
    public String dashboard(@RequestParam(name = "range", defaultValue = "7d") String range,
                            HttpSession session, Model model) {
        if (!sessionService.hasRole(session, "MANAGER", "ADMIN")) return "redirect:/login";

        model.addAttribute("summary", dashboardService.getSummary());
        model.addAttribute("chart", dashboardService.getRevenueChart(range));
        model.addAttribute("upcoming", dashboardService.getUpcomingShowtimes());
        model.addAttribute("bookings", dashboardService.getRecentBookings());
        model.addAttribute("todayText", dashboardService.getTodayText());
        model.addAttribute("shiftText", dashboardService.getShiftText());
        return "manager/dashboard";
    }
}

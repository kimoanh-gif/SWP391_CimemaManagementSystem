package com.cinema.controller;

import com.cinema.model.AuthenticatedUser;
import com.cinema.service.SessionService;
import com.cinema.service.StaffOperationsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Role-aware dashboard routing. The HTML files only render a welcome card;
 * the route is what enforces authorization.
 */
@Controller
public class RoleDashboardController {

    private final SessionService sessionService;
    private final StaffOperationsService staffOperationsService;

    public RoleDashboardController(SessionService sessionService, StaffOperationsService staffOperationsService) {
        this.sessionService = sessionService;
        this.staffOperationsService = staffOperationsService;
    }

    @GetMapping({"/customer", "/customer/", "/customer/dashboard"})
    public String customer(HttpSession session, Model model) {
        if (!sessionService.hasRole(session, "CUSTOMER", "ADMIN", "MANAGER", "STAFF"))
            return "redirect:/login";
        model.addAttribute("user", sessionService.getCurrentUser(session));
        return "customer/dashboard";
    }

    @GetMapping({"/staff", "/staff/", "/staff/dashboard"})
    public String staff(HttpSession session, Model model) {
        if (!sessionService.hasRole(session, "STAFF", "MANAGER", "ADMIN"))
            return "redirect:/login";
        model.addAttribute("user", sessionService.getCurrentUser(session));
        model.addAttribute("staffDashboard", staffOperationsService.dashboard(
                sessionService.currentAccountId(session), sessionService.currentRole(session)));
        return "staff/dashboard";
    }

    // /manager/dashboard is handled by ManagerDashboardController with full operational statistics

    /** Logged-in header fragment - exposes the current user. */
    @GetMapping("/me")
    public String me(HttpSession session, Model model) {
        AuthenticatedUser u = sessionService.getCurrentUser(session);
        model.addAttribute("user", u);
        return u == null ? "redirect:/login" : "auth/_me";
    }
}

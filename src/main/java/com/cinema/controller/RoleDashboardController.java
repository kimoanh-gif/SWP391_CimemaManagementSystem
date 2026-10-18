package com.cinema.controller;

import com.cinema.model.AuthenticatedUser;
import com.cinema.service.SessionService;
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

    public RoleDashboardController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/customer/dashboard")
    public String customer(HttpSession session, Model model) {
        if (!sessionService.hasRole(session, "CUSTOMER", "ADMIN", "MANAGER", "STAFF"))
            return "redirect:/login";
        model.addAttribute("user", sessionService.getCurrentUser(session));
        return "customer/dashboard";
    }

    @GetMapping("/staff/dashboard")
    public String staff(HttpSession session, Model model) {
        if (!sessionService.hasRole(session, "STAFF", "MANAGER", "ADMIN"))
            return "redirect:/login";
        model.addAttribute("user", sessionService.getCurrentUser(session));
        return "staff/dashboard";
    }

    @GetMapping("/manager/dashboard")
    public String manager(HttpSession session, Model model) {
        if (!sessionService.hasRole(session, "MANAGER", "ADMIN"))
            return "redirect:/login";
        model.addAttribute("user", sessionService.getCurrentUser(session));
        return "manager/dashboard";
    }

    /** Logged-in header fragment – exposes the current user. */
    @GetMapping("/me")
    public String me(HttpSession session, Model model) {
        AuthenticatedUser u = sessionService.getCurrentUser(session);
        model.addAttribute("user", u);
        return u == null ? "redirect:/login" : "auth/_me";
    }
}

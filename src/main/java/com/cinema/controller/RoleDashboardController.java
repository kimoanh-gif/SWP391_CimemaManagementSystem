package com.cinema.controller;

import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RoleDashboardController {

    private final SessionService sessionService;

    public RoleDashboardController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/customer/dashboard")
    public String customer(HttpSession session) {
        return sessionService.hasRole(session, "CUSTOMER")
                ? "customer/dashboard" : "redirect:/login";
    }

    @GetMapping("/staff/dashboard")
    public String staff(HttpSession session) {
        return sessionService.hasRole(session, "STAFF")
                ? "staff/dashboard" : "redirect:/login";
    }

    @GetMapping("/manager/dashboard")
    public String manager(HttpSession session) {
        return sessionService.hasRole(session, "MANAGER")
                ? "manager/dashboard" : "redirect:/login";
    }

    @GetMapping("/admin/dashboard")
    public String admin(HttpSession session) {
        return sessionService.hasRole(session, "ADMIN")
                ? "admin/dashboard" : "redirect:/login";
    }
}

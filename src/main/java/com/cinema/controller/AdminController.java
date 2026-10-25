package com.cinema.controller;

import com.cinema.model.AuthenticatedUser;
import com.cinema.service.CustomerAccountService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private CustomerAccountService customerAccountService;

    @Autowired
    private com.cinema.service.AdminDashboardService adminDashboardService;

    private void addUserToModel(Model model, HttpSession session) {
        AuthenticatedUser user = sessionService.getCurrentUser(session);
        model.addAttribute("user", user);
        model.addAttribute("fullName", user != null ? user.getFullName() : null);
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session,
                            @RequestParam(defaultValue = "day") String period,
                            @RequestParam(required = false) String from,
                            @RequestParam(required = false) String to) {
        addUserToModel(model, session);
        try {
            try {
                model.addAttribute("dashboard", adminDashboardService.load(period, from, to));
            } catch (IllegalArgumentException ex) {
                model.addAttribute("revenueFilterError", ex.getMessage());
                model.addAttribute("dashboard", adminDashboardService.load(period));
            }
        } catch (org.springframework.dao.DataAccessException ex) {
            model.addAttribute("dashboardError", "Unable to load dashboard data. Please try again later.");
        }
        return "admin/dashboard";
    }

    @GetMapping("/customers")
    public String customers(Model model, HttpSession session) {
        addUserToModel(model, session);
        model.addAttribute("customerStats", Map.of(
                "total",     customerAccountService.getTotalCount(),
                "active",    customerAccountService.getActiveCount(),
                "suspended", customerAccountService.getSuspendedCount(),
                "pending",   customerAccountService.getPendingCount()
        ));
        return "admin/customers";
    }

    @GetMapping("/staffs")
    public String staffs(Model model, HttpSession session) {
        addUserToModel(model, session);
        return "admin/staffs";
    }

    @GetMapping("/roles")
    public String roles(Model model, HttpSession session) {
        addUserToModel(model, session);
        return "admin/roles";
    }

    @GetMapping("/config")
    public String config(Model model, HttpSession session) {
        addUserToModel(model, session);
        return "admin/config";
    }
}

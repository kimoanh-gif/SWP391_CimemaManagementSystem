package com.cinema.controller;

import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaffFeaturesController {

    private final SessionService sessionService;

    public StaffFeaturesController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/staff/news")
    public String newsEditor(HttpSession session, Model model) {
        if (!sessionService.hasRole(session, "STAFF", "MANAGER", "ADMIN")) {
            return "redirect:/login";
        }
        model.addAttribute("user", sessionService.getCurrentUser(session));
        return "staff/news";
    }
}

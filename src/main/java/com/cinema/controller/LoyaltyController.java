package com.cinema.controller;

import com.cinema.model.LoyaltyInfo;
import com.cinema.service.LoyaltyService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Screen: Loyalty Information (UC18 – View Loyalty Information).
 * URL: /customer/loyalty
 * The old link /customer/profile/loyalty stays in CustomerProfileController (owned by another
 * member); the same data is added to that page by {@link LoyaltyModelAdvice}.
 * Customer only, read-only.
 *
 * Assignee: Nguyễn Đức Anh (Iter3)
 */
@Controller
public class LoyaltyController {

    private static final Logger log = LoggerFactory.getLogger(LoyaltyController.class);

    private final LoyaltyService loyaltyService;
    private final SessionService sessionService;

    public LoyaltyController(LoyaltyService loyaltyService, SessionService sessionService) {
        this.loyaltyService = loyaltyService;
        this.sessionService = sessionService;
    }

    @GetMapping("/customer/loyalty")
    public String viewLoyalty(HttpSession session, Model model) {
        if (!sessionService.hasRole(session, "CUSTOMER")) return "redirect:/login";
        Integer accountId = sessionService.currentAccountId(session);
        if (accountId == null) return "redirect:/login";

        model.addAttribute("currentUser", sessionService.getCurrentUser(session));
        model.addAttribute("loadError", false);
        try {
            LoyaltyInfo info = loyaltyService.getLoyaltyInfo(accountId);
            model.addAttribute("loyalty", info);
            model.addAttribute("availablePoints", info.availablePoints());
        } catch (DataAccessException | IllegalArgumentException ex) {
            // UC18 E1 – loyalty data unavailable.
            log.error("Cannot load loyalty information for account {}", accountId, ex);
            model.addAttribute("loadError", true);
        }
        return "customer/loyalty";
    }
}

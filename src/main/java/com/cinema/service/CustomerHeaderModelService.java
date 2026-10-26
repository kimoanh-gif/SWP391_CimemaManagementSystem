package com.cinema.service;

import com.cinema.dao.CustomerDAO;
import com.cinema.model.AuthenticatedUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

/**
 * Adds the logged-in user and the Customer point balance used by the public header
 * (fragments/cinema-header-footer :: header). Shared by the public screens so each
 * controller stays small and does not repeat this code.
 */
@Service
public class CustomerHeaderModelService {

    private final SessionService sessionService;
    private final CustomerDAO customerDAO;

    public CustomerHeaderModelService(SessionService sessionService, CustomerDAO customerDAO) {
        this.sessionService = sessionService;
        this.customerDAO = customerDAO;
    }

    public void addHeaderAttributes(HttpSession session, Model model, String activeNav) {
        AuthenticatedUser user = sessionService.getCurrentUser(session);
        model.addAttribute("currentUser", user);
        model.addAttribute("activeNav", activeNav);
        if (user != null && "CUSTOMER".equalsIgnoreCase(user.getRole())) {
            customerDAO.findByAccountId(user.getAccountId()).ifPresent(c -> {
                model.addAttribute("availablePoints", c.getAvailablePoints());
                model.addAttribute("tierPoints", c.getTierPoints());
            });
        }
    }
}

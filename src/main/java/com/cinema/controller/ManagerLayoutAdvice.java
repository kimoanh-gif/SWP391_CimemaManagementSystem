package com.cinema.controller;

import com.cinema.model.AuthenticatedUser;
import com.cinema.model.ManagerHeaderInfo;
import com.cinema.service.ManagerDashboardService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Adds the data used by the shared Manager layout (sidebar + top bar) to every Manager page. */
@ControllerAdvice(assignableTypes = {ManagerDashboardController.class, ManagerNewsController.class})
public class ManagerLayoutAdvice {

    private static final Logger log = LoggerFactory.getLogger(ManagerLayoutAdvice.class);

    private final ManagerDashboardService dashboardService;
    private final SessionService sessionService;

    public ManagerLayoutAdvice(ManagerDashboardService dashboardService, SessionService sessionService) {
        this.dashboardService = dashboardService;
        this.sessionService = sessionService;
    }

    @ModelAttribute("header")
    public ManagerHeaderInfo header(HttpSession session) {
        AuthenticatedUser user = sessionService.getCurrentUser(session);
        try {
            return dashboardService.getHeaderInfo(user);
        } catch (DataAccessException e) {
            log.warn("Could not load manager header info: {}", e.getMessage());
            return new ManagerHeaderInfo(0, 0,
                    user == null ? null : user.getFullName(), user == null ? null : user.getRole());
        }
    }
}

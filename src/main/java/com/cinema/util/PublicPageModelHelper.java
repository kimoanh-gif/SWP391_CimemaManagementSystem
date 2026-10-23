package com.cinema.util;

import com.cinema.dao.CustomerDAO;
import com.cinema.model.AuthenticatedUser;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.function.Supplier;

/**
 * Shared model attributes for the public customer-facing screens
 * (Home Page, Movie List, Movie Details, Showtime List, Promotions &amp; News).
 *
 * Keeps every screen controller small: each controller only loads its own data
 * and calls this helper for the header (signed-in user, loyalty points, active menu).
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Component
public class PublicPageModelHelper {

    /** Model flag read by the templates to show the "could not load data — retry" banner. */
    public static final String LOAD_ERROR_ATTRIBUTE = "loadError";

    private static final Logger log = LoggerFactory.getLogger(PublicPageModelHelper.class);

    private final SessionService sessionService;
    private final CustomerDAO customerDAO;

    public PublicPageModelHelper(SessionService sessionService, CustomerDAO customerDAO) {
        this.sessionService = sessionService;
        this.customerDAO = customerDAO;
    }

    /** Adds currentUser, availablePoints, tierPoints and activeNav to the model. */
    public AuthenticatedUser populateHeader(HttpSession session, Model model, String activeNav) {
        AuthenticatedUser user = sessionService.getCurrentUser(session);
        model.addAttribute("currentUser", user);
        model.addAttribute("activeNav", activeNav);

        if (isCustomer(user)) {
            try {
                customerDAO.findByAccountId(user.getAccountId()).ifPresent(c -> {
                    model.addAttribute("availablePoints", c.getAvailablePoints());
                    model.addAttribute("tierPoints", c.getTierPoints());
                });
            } catch (DataAccessException ex) {
                // Loyalty points are optional on public pages; never fail the page because of them.
                log.warn("Could not load loyalty points for account {}", user.getAccountId(), ex);
            }
        }
        return user;
    }

    public boolean isCustomer(AuthenticatedUser user) {
        return user != null && "CUSTOMER".equalsIgnoreCase(user.getRole());
    }

    /**
     * Runs a data loader and returns the fallback when the database cannot be reached,
     * flagging the model so the page shows an error message with a retry action
     * (exception flow E1 of UC01–UC04) instead of the generic error page.
     */
    public <T> T loadSafely(Model model, Supplier<T> loader, T fallback) {
        try {
            T value = loader.get();
            return value != null ? value : fallback;
        } catch (DataAccessException ex) {
            log.error("Failed to load data for a public page", ex);
            model.addAttribute(LOAD_ERROR_ATTRIBUTE, true);
            return fallback;
        }
    }
}

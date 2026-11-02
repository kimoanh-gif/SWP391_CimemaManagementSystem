package com.cinema.controller;

import com.cinema.model.LoyaltyInfo;
import com.cinema.service.LoyaltyService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds the loyalty data to GET /customer/profile/loyalty (the link in the site header).
 *
 * That URL is handled by CustomerProfileController (owned by another member), which renders
 * the customer/loyalty template with only the profile. This advice adds "loyalty" and
 * "loadError" so the same Loyalty Information screen is shown, without changing that controller.
 * On GET /customer/booking/promotions (CustomerBookingController, Voucher / Promotion Selection)
 * it adds "memberDiscountHint". It does nothing for any other request.
 *
 * The services are looked up lazily (ObjectProvider) so that @WebMvcTest slices of other
 * controllers, which load every @ControllerAdvice but not the services, still start.
 *
 * Assignee: Nguyễn Đức Anh (Iter3)
 */
@ControllerAdvice(assignableTypes = {CustomerProfileController.class, CustomerBookingController.class})
public class LoyaltyModelAdvice {

    private static final Logger log = LoggerFactory.getLogger(LoyaltyModelAdvice.class);
    static final String PAGE_PATH = "/customer/profile/loyalty";
    static final String VOUCHER_STEP_PATH = "/customer/booking/promotions";

    private final ObjectProvider<LoyaltyService> loyaltyServiceProvider;
    private final ObjectProvider<SessionService> sessionServiceProvider;

    public LoyaltyModelAdvice(ObjectProvider<LoyaltyService> loyaltyServiceProvider, ObjectProvider<SessionService> sessionServiceProvider) {
        this.loyaltyServiceProvider = loyaltyServiceProvider;
        this.sessionServiceProvider = sessionServiceProvider;
    }

    @ModelAttribute
    public void addLoyalty(HttpServletRequest request, HttpSession session, Model model) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) return;
        if (isPage(request, VOUCHER_STEP_PATH)) {
            addMemberDiscountHint(session, model);
            return;
        }
        if (!isLoyaltyPage(request)) {
            return;
        }
        LoyaltyService loyaltyService = loyaltyServiceProvider.getIfAvailable();
        SessionService sessionService = sessionServiceProvider.getIfAvailable();
        if (loyaltyService == null || sessionService == null) return;
        if (!sessionService.hasRole(session, "CUSTOMER")) return;
        Integer accountId = sessionService.currentAccountId(session);
        if (accountId == null) return;

        model.addAttribute("loadError", false);
        try {
            LoyaltyInfo info = loyaltyService.getLoyaltyInfo(accountId);
            model.addAttribute("loyalty", info);
        } catch (RuntimeException ex) {
            // UC18 E1 – loyalty data unavailable.
            log.error("Cannot load loyalty information for account {}", accountId, ex);
            model.addAttribute("loadError", true);
        }
    }

    /**
     * Voucher / Promotion Selection step: tells the Customer that their rank discount is applied at
     * checkout only when no voucher is used (vouchers and member discounts are not combined).
     */
    private void addMemberDiscountHint(HttpSession session, Model model) {
        LoyaltyService loyaltyService = loyaltyServiceProvider.getIfAvailable();
        SessionService sessionService = sessionServiceProvider.getIfAvailable();
        if (loyaltyService == null || sessionService == null || !sessionService.hasRole(session, "CUSTOMER")) return;
        Integer accountId = sessionService.currentAccountId(session);
        if (accountId == null) return;
        try {
            LoyaltyInfo info = loyaltyService.getLoyaltyInfo(accountId);
            String terms = LoyaltyService.describeMemberDiscount(info.currentRank());
            if (terms != null) {
                model.addAttribute("memberDiscountHint", "Your " + info.getCurrentRankName() + " benefit: " + terms
                        + ". It is applied at checkout when you continue without a voucher; applying a voucher replaces it.");
            }
        } catch (RuntimeException ex) {
            log.warn("Cannot load member discount hint for account {}", accountId, ex);
        }
    }

    private static boolean isPage(HttpServletRequest request, String pagePath) {
        String uri = request.getRequestURI();
        String context = request.getContextPath() != null ? request.getContextPath() : "";
        if (uri == null) return false;
        String path = uri.substring(Math.min(context.length(), uri.length()));
        return path.equals(pagePath) || path.equals(pagePath + "/");
    }

    private static boolean isLoyaltyPage(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath() != null ? request.getContextPath() : "";
        if (uri == null) return false;
        String path = uri.substring(Math.min(context.length(), uri.length()));
        return path.equals(PAGE_PATH) || path.equals(PAGE_PATH + "/");
    }
}

package com.cinema.controller;

import com.cinema.service.SessionService;
import com.cinema.service.VoucherSelectionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds the voucher data to GET /customer/booking/promotions.
 *
 * That URL is handled by CustomerBookingController (owned by another member), which only
 * puts "summary" into the model. This advice adds "pricing", "vouchers" and "appliedVoucher"
 * for the Voucher / Promotion Selection template without changing that controller.
 * It does nothing for any other request.
 *
 * Assignee: Nguyễn Đức Anh (Iter3)
 */
@ControllerAdvice(assignableTypes = CustomerBookingController.class)
public class VoucherSelectionModelAdvice {

    private static final Logger log = LoggerFactory.getLogger(VoucherSelectionModelAdvice.class);
    static final String PAGE_PATH = "/customer/booking/promotions";

    private final VoucherSelectionService voucherService;
    private final SessionService sessionService;

    public VoucherSelectionModelAdvice(VoucherSelectionService voucherService, SessionService sessionService) {
        this.voucherService = voucherService;
        this.sessionService = sessionService;
    }

    @ModelAttribute
    public void addVoucherSelection(HttpServletRequest request, HttpSession session, Model model) {
        if (!"GET".equalsIgnoreCase(request.getMethod()) || !isVoucherPage(request)) {
            return;
        }
        if (!sessionService.hasRole(session, "CUSTOMER")) return;
        Integer accountId = sessionService.currentAccountId(session);
        Object bookingId = session.getAttribute(VoucherSelectionController.SESSION_BOOKING_ID);
        if (accountId == null || !(bookingId instanceof Integer id)) return;

        try {
            VoucherSelectionService.SelectionView view = voucherService.getSelectionView(accountId, id);
            model.addAttribute("pricing", view.pricing());
            model.addAttribute("vouchers", view.vouchers());
            model.addAttribute("appliedVoucher", view.appliedVoucher());
        } catch (RuntimeException ex) {
            // The page still renders (voucher code input + Skip); the booking controller
            // handles expired / missing bookings itself.
            log.warn("Cannot load vouchers for booking {}: {}", id, ex.getMessage());
        }
    }

    private static boolean isVoucherPage(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath() != null ? request.getContextPath() : "";
        if (uri == null) return false;
        String path = uri.substring(Math.min(context.length(), uri.length()));
        return path.equals(PAGE_PATH) || path.equals(PAGE_PATH + "/");
    }
}

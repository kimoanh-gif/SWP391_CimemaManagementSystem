package com.cinema.controller;

import com.cinema.model.VoucherOption;
import com.cinema.service.CustomerBookingService;
import com.cinema.service.CustomerBookingService.BookingException;
import com.cinema.service.SessionService;
import com.cinema.service.VoucherSelectionService;
import com.cinema.service.VoucherSelectionService.VoucherException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Screen: Voucher / Promotion Selection – step between Food &amp; Beverage and Checkout.
 * URLs handled here:
 *  POST /customer/booking/promotions/apply     validate and apply a voucher code
 *  POST /customer/booking/promotions/remove    remove the applied voucher
 *  POST /customer/booking/promotions/skip      continue without voucher
 *  POST /customer/booking/promotions/continue  continue to checkout (re-validates the voucher)
 *
 * The page itself (GET /customer/booking/promotions) stays in CustomerBookingController
 * (owned by another member); its voucher data is added by {@link VoucherSelectionModelAdvice}.
 *
 * Assignee: Nguyễn Đức Anh (Iter3)
 */
@Controller
@RequestMapping("/customer/booking/promotions")
public class VoucherSelectionController {

    /** Same session keys as CustomerBookingController. */
    static final String SESSION_BOOKING_ID = "customerPendingBookingId";
    static final String SESSION_VOUCHER_CODE = "customerBookingVoucherCode";

    private static final String PAGE = "redirect:/customer/booking/promotions";
    private static final String CHECKOUT = "redirect:/customer/booking/checkout";
    private static final String SEATS = "redirect:/customer/booking/seats";

    private final VoucherSelectionService voucherService;
    private final CustomerBookingService bookingService;
    private final SessionService sessionService;

    public VoucherSelectionController(VoucherSelectionService voucherService,
                                      CustomerBookingService bookingService,
                                      SessionService sessionService) {
        this.voucherService = voucherService;
        this.bookingService = bookingService;
        this.sessionService = sessionService;
    }

    @PostMapping("/apply")
    public String applyVoucher(@RequestParam(required = false) String voucherCode,
                               HttpSession session, RedirectAttributes redirectAttributes) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        Integer bookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
        if (bookingId == null) return SEATS;

        try {
            bookingService.getActiveBooking(bookingId, accountId);
            VoucherOption applied = voucherService.applyVoucher(accountId, bookingId, voucherCode);
            session.setAttribute(SESSION_VOUCHER_CODE, applied.getCode());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Voucher " + applied.getCode() + " applied.");
        } catch (BookingException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return SEATS;
        } catch (VoucherException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            redirectAttributes.addFlashAttribute("enteredCode", voucherCode);
        }
        return PAGE;
    }

    @PostMapping("/remove")
    public String removeVoucher(HttpSession session, RedirectAttributes redirectAttributes) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        Integer bookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
        if (bookingId == null) return SEATS;

        try {
            voucherService.removeVoucher(accountId, bookingId);
            session.removeAttribute(SESSION_VOUCHER_CODE);
            redirectAttributes.addFlashAttribute("successMessage", "Voucher removed.");
        } catch (VoucherException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return PAGE;
    }

    @PostMapping("/skip")
    public String skipVoucher(HttpSession session, RedirectAttributes redirectAttributes) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        Integer bookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
        if (bookingId == null) return SEATS;

        try {
            voucherService.removeVoucher(accountId, bookingId);
            session.removeAttribute(SESSION_VOUCHER_CODE);
            return CHECKOUT;
        } catch (VoucherException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return PAGE;
        }
    }

    @PostMapping("/continue")
    public String continueToCheckout(HttpSession session, RedirectAttributes redirectAttributes) {
        Integer accountId = customerAccountId(session);
        if (accountId == null) return "redirect:/login";
        Integer bookingId = (Integer) session.getAttribute(SESSION_BOOKING_ID);
        if (bookingId == null) return SEATS;

        try {
            bookingService.getActiveBooking(bookingId, accountId);
            Optional<String> code = voucherService.confirmForCheckout(accountId, bookingId);
            if (code.isPresent()) {
                session.setAttribute(SESSION_VOUCHER_CODE, code.get());
            } else {
                session.removeAttribute(SESSION_VOUCHER_CODE);
            }
            return CHECKOUT;
        } catch (BookingException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return SEATS;
        } catch (VoucherException exception) {
            session.removeAttribute(SESSION_VOUCHER_CODE);
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return PAGE;
        }
    }

    private Integer customerAccountId(HttpSession session) {
        return sessionService.hasRole(session, "CUSTOMER")
                ? sessionService.currentAccountId(session)
                : null;
    }
}
